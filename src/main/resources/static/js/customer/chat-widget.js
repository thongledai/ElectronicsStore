/**
 * chat-widget.js - Real-time Customer Live Chat (WebSocket STOMP over SockJS)
 */
document.addEventListener('DOMContentLoaded', function () {
    const bubble = document.getElementById('customerChatBubble');
    const chatBox = document.getElementById('customerChatBox');
    const btnMinimize = document.getElementById('btnMinimizeChat');
    const chatBody = document.getElementById('customerChatBody');
    const inputMsg = document.getElementById('chatInputMessage');
    const btnSend = document.getElementById('btnSendChatMessage');
    const btnUploadImg = document.getElementById('btnUploadChatImg');
    const fileInput = document.getElementById('chatFileInput');
    const unreadBadge = document.getElementById('chatUnreadBadge');
    const supportStatusText = document.getElementById('chatSupportStatusText');

    if (!bubble || !chatBox) return;

    let currentRoomId = null;
    let stompClient = null;
    let isConnected = false;
    let unreadCount = 0;
    let currentUserId = null;

    // Lấy thông tin user hiện tại từ localStorage nếu có
    try {
        const u = JSON.parse(localStorage.getItem('technova_user') || '{}');
        if (u && u.id) currentUserId = u.id;
    } catch (e) {}

    // Toggle chat box
    bubble.addEventListener('click', () => {
        const isOpen = chatBox.classList.contains('open');
        if (!isOpen) {
            chatBox.classList.add('open');
            unreadCount = 0;
            updateUnreadBadge();
            if (!currentRoomId) {
                initChatRoom();
            }
            if (inputMsg) inputMsg.focus();
        } else {
            chatBox.classList.remove('open');
        }
    });

    if (btnMinimize) {
        btnMinimize.addEventListener('click', () => {
            chatBox.classList.remove('open');
        });
    }

    function updateUnreadBadge() {
        if (!unreadBadge) return;
        if (unreadCount > 0) {
            unreadBadge.textContent = unreadCount > 9 ? '9+' : unreadCount;
            unreadBadge.classList.remove('d-none');
        } else {
            unreadBadge.classList.add('d-none');
        }
    }

    async function initChatRoom() {
        try {
            const res = await fetch('/api/customer/chat/room', {
                method: 'POST',
                headers: { 'Accept': 'application/json' }
            });
            const resData = await res.json();
            if (!resData.success || !resData.data) {
                console.error('Cannot init chat room:', resData.message);
                return;
            }

            const room = resData.data;
            currentRoomId = room.id;
            if (room.status === 'CLOSED') {
                if (supportStatusText) supportStatusText.textContent = 'Phiên hỗ trợ đã kết thúc';
            } else if (room.employeeName && supportStatusText) {
                supportStatusText.textContent = 'Nhân Viên CSKH đang hỗ trợ';
            }

            // Tải lịch sử tin nhắn
            await loadMessageHistory(currentRoomId);

            // Kết nối WebSocket
            connectWebSocket(currentRoomId);
        } catch (err) {
            console.error('Error init chat room:', err);
        }
    }

    async function loadMessageHistory(roomId) {
        try {
            const res = await fetch(`/api/customer/chat/room/${roomId}/messages`);
            const resData = await res.json();
            if (!resData.success || !resData.data) return;

            chatBody.innerHTML = `
                <div class="chat-msg chat-msg-system">
                    👋 Chào mừng bạn! Nhân Viên CSKH TechNova luôn sẵn sàng hỗ trợ bạn.
                </div>
            `;

            resData.data.forEach(appendMessage);
            scrollToBottom();
        } catch (err) {
            console.error('Error loading chat history:', err);
        }
    }

    function connectWebSocket(roomId) {
        if (isConnected && stompClient) return;

        try {
            const socket = new SockJS('/ws');
            stompClient = new StompJs.Client({
                webSocketFactory: () => socket,
                reconnectDelay: 5000,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000
            });

            stompClient.onConnect = function () {
                isConnected = true;
                stompClient.subscribe('/topic/chat/' + roomId, function (messageOutput) {
                    try {
                        const data = JSON.parse(messageOutput.body);
                        if (data.type === 'EMPLOYEE_JOINED') {
                            appendSystemMessage(data.message || 'Nhân Viên CSKH đã tham gia hỗ trợ');
                            if (supportStatusText) supportStatusText.textContent = 'Nhân Viên CSKH đang hỗ trợ';
                        } else if (data.type === 'ROOM_CLOSED') {
                            appendSystemMessage(data.message || 'Cuộc hội thoại đã kết thúc. Nếu cần hỗ trợ thêm, bạn chỉ cần gửi tin nhắn mới bên dưới nhé!');
                            if (supportStatusText) supportStatusText.textContent = 'Phiên hỗ trợ đã kết thúc';
                        } else {
                            // Tin nhắn thông thường
                            appendMessage(data);
                            if (!chatBox.classList.contains('open')) {
                                unreadCount++;
                                updateUnreadBadge();
                            }
                        }
                        scrollToBottom();
                    } catch (e) {
                        console.error('Parse WS message error:', e);
                    }
                });
            };

            stompClient.onStompError = function (frame) {
                console.warn('STOMP error:', frame);
            };

            stompClient.activate();
        } catch (e) {
            console.warn('WebSocket connect error:', e);
        }
    }

    function appendMessage(msg) {
        if (!chatBody) return;

        const isMe = currentUserId && String(msg.senderId) === String(currentUserId);
        const isCustomer = (msg.senderRole || '').toUpperCase().includes('CUSTOMER');
        // Phân loại: Khách hàng đang ở view này, nếu tin nhắn do CUSTOMER gửi thì là tin của mình (hoặc dựa trên senderId)
        const isSentByMe = isMe || isCustomer;

        const isImage = msg.messageType === 'IMAGE';
        const div = document.createElement('div');
        div.className = `chat-msg ${isImage ? 'chat-msg-image' : ''} ${isSentByMe ? 'chat-msg-sent' : 'chat-msg-received'}`;

        let contentHtml = '';
        if (msg.messageType === 'IMAGE') {
            contentHtml = `
                <a href="${escapeHtml(msg.content)}" target="_blank" rel="noopener noreferrer">
                    <img src="${escapeHtml(msg.content)}" class="chat-msg-img" alt="Ảnh gửi">
                </a>
            `;
        } else {
            contentHtml = `<div>${escapeHtml(msg.content)}</div>`;
        }

        const timeStr = formatChatTime(msg.createdAt);
        const displayName = isSentByMe ? '' : (msg.senderName || 'Nhân Viên CSKH');
        div.innerHTML = `
            ${!isSentByMe ? `<div class="fw-bold small text-primary mb-1" style="font-size: 0.72rem;">${escapeHtml(displayName)}</div>` : ''}
            ${contentHtml}
            <div class="chat-msg-time">${timeStr}</div>
        `;

        chatBody.appendChild(div);
    }

    function appendSystemMessage(text) {
        if (!chatBody) return;
        const div = document.createElement('div');
        div.className = 'chat-msg chat-msg-system';
        div.textContent = text;
        chatBody.appendChild(div);
    }

    function formatChatTime(dateStr) {
        if (!dateStr) return '';
        try {
            const d = new Date(dateStr);
            const h = String(d.getHours()).padStart(2, '0');
            const m = String(d.getMinutes()).padStart(2, '0');
            return `${h}:${m}`;
        } catch {
            return '';
        }
    }

    function scrollToBottom() {
        if (chatBody) {
            chatBody.scrollTop = chatBody.scrollHeight;
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // Gửi tin nhắn
    async function doSendMessage() {
        if (!inputMsg) return;
        const text = inputMsg.value.trim();
        if (!text) return;
        if (!currentRoomId) return;

        inputMsg.value = '';

        try {
            const payload = {
                roomId: currentRoomId,
                content: text,
                messageType: 'TEXT'
            };

            await fetch('/api/customer/chat/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(payload)
            });
        } catch (err) {
            console.error('Send message error:', err);
        }
    }

    if (btnSend) {
        btnSend.addEventListener('click', doSendMessage);
    }
    if (inputMsg) {
        inputMsg.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                doSendMessage();
            }
        });
    }

    // Gửi hình ảnh
    if (btnUploadImg && fileInput) {
        btnUploadImg.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', async () => {
            const file = fileInput.files && fileInput.files[0];
            if (!file) return;

            if (!file.type.startsWith('image/')) {
                alert('Vui lòng chọn file hình ảnh.');
                return;
            }
            if (file.size > 5 * 1024 * 1024) {
                alert('Kích thước ảnh tối đa 5MB.');
                return;
            }

            const formData = new FormData();
            formData.append('file', file);

            try {
                btnUploadImg.disabled = true;
                const uploadRes = await fetch('/api/customer/chat/upload-image', {
                    method: 'POST',
                    body: formData
                });
                const uploadData = await uploadRes.json();
                if (uploadData.success && uploadData.data) {
                    const imgUrl = uploadData.data;
                    await fetch('/api/customer/chat/message', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json'
                        },
                        body: JSON.stringify({
                            roomId: currentRoomId,
                            content: imgUrl,
                            messageType: 'IMAGE'
                        })
                    });
                } else {
                    alert(uploadData.message || 'Tải ảnh thất bại');
                }
            } catch (err) {
                console.error('Upload chat image error:', err);
            } finally {
                btnUploadImg.disabled = false;
                fileInput.value = '';
            }
        });
    }
});
