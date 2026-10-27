/**
 * chat.js - Dashboard hỗ trợ khách hàng trực tuyến cho nhân viên (Live Chat)
 */
document.addEventListener('DOMContentLoaded', function () {
    let stompClient = null;
    let isConnected = false;
    let allRooms = [];
    let currentRoomId = null;
    let activeRoomSubscription = null;
    let currentEmployeeId = null;

    // Lấy thông tin nhân viên hiện tại
    try {
        const u = JSON.parse(localStorage.getItem('technova_user') || '{}');
        if (u && u.id) currentEmployeeId = u.id;
    } catch (e) {}

    const roomsLoading = document.getElementById('roomsLoading');
    const roomsEmpty = document.getElementById('roomsEmpty');
    const roomsList = document.getElementById('chatRoomsList');
    const searchInput = document.getElementById('searchCustomerChat');
    const btnRefresh = document.getElementById('btnRefreshRooms');
    const sidebarChatBadge = document.getElementById('sidebarOpenChatBadge');

    const noSelectedPlaceholder = document.getElementById('chatNoSelectedPlaceholder');
    const conversationWrapper = document.getElementById('chatConversationWrapper');
    const activeAvatar = document.getElementById('activeCustomerAvatar');
    const activeName = document.getElementById('activeCustomerName');
    const activeEmail = document.getElementById('activeCustomerEmail');
    const btnAssignMe = document.getElementById('btnAssignMe');
    const btnCloseRoom = document.getElementById('btnCloseRoom');

    const messagesStream = document.getElementById('employeeMessagesStream');
    const chatInput = document.getElementById('empChatInput');
    const btnSend = document.getElementById('btnEmpSendMsg');
    const btnUploadImg = document.getElementById('btnEmpUploadImg');
    const fileInput = document.getElementById('empChatFileInput');

    // 1. Tải danh sách phòng chat
    async function loadRooms() {
        roomsLoading.classList.remove('d-none');
        roomsEmpty.classList.add('d-none');
        roomsList.innerHTML = '';

        try {
            const res = await fetch('/api/employee/chat/rooms');
            const resData = await res.json();

            roomsLoading.classList.add('d-none');

            if (!resData.success || !resData.data || resData.data.length === 0) {
                roomsEmpty.classList.remove('d-none');
                allRooms = [];
                updateSidebarBadge(0);
                return;
            }

            allRooms = resData.data;
            updateSidebarBadge(allRooms.length);
            renderRoomsList(allRooms);
        } catch (err) {
            console.error('Load rooms error:', err);
            roomsLoading.classList.add('d-none');
            roomsEmpty.classList.remove('d-none');
        }
    }

    function updateSidebarBadge(count) {
        if (!sidebarChatBadge) return;
        sidebarChatBadge.textContent = count;
        if (count > 0) {
            sidebarChatBadge.classList.remove('d-none');
        } else {
            sidebarChatBadge.classList.add('d-none');
        }
    }

    function renderRoomsList(rooms) {
        if (!rooms || rooms.length === 0) {
            roomsEmpty.classList.remove('d-none');
            roomsList.innerHTML = '';
            return;
        }

        roomsEmpty.classList.add('d-none');
        roomsList.innerHTML = rooms.map(r => {
            const initial = (r.customerName || 'K').charAt(0).toUpperCase();
            const avatarHtml = r.customerAvatar
                ? `<div class="tgdd-avatar" style="width: 40px; height: 40px;"><img src="${escapeHtml(r.customerAvatar)}" alt=""></div>`
                : `<div class="tgdd-avatar" style="width: 40px; height: 40px;">${escapeHtml(initial)}</div>`;

            const isActive = currentRoomId && String(r.id) === String(currentRoomId);
            const unreadBadgeHtml = r.unreadCount > 0
                ? `<span class="badge bg-danger rounded-pill">${r.unreadCount}</span>`
                : '';

            const timeStr = r.lastMessageTime ? formatTime(r.lastMessageTime) : '';

            return `
                <button type="button" class="list-group-item list-group-item-action p-3 d-flex align-items-center gap-3 border-0 border-bottom js-room-item ${isActive ? 'bg-primary-subtle' : ''}" data-room-id="${r.id}">
                    ${avatarHtml}
                    <div class="flex-grow-1 min-w-0 text-start">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="fw-bold text-dark text-truncate small">${escapeHtml(r.customerName || 'Khách hàng')}</span>
                            <span class="text-muted" style="font-size: 0.72rem;">${timeStr}</span>
                        </div>
                        <div class="d-flex justify-content-between align-items-center">
                            <span class="text-muted text-truncate small" style="max-width: 170px;">
                                ${escapeHtml(r.lastMessage || 'Cuộc trò chuyện mới')}
                            </span>
                            ${unreadBadgeHtml}
                        </div>
                    </div>
                </button>
            `;
        }).join('');

        // Click chọn phòng
        roomsList.querySelectorAll('.js-room-item').forEach(btn => {
            btn.addEventListener('click', () => {
                const roomId = btn.getAttribute('data-room-id');
                selectRoom(roomId);
            });
        });
    }

    // 2. Chọn một phòng chat
    async function selectRoom(roomId) {
        currentRoomId = roomId;
        const room = allRooms.find(r => String(r.id) === String(roomId));
        if (!room) return;

        // Cập nhật active class
        roomsList.querySelectorAll('.js-room-item').forEach(b => {
            if (b.getAttribute('data-room-id') === String(roomId)) {
                b.classList.add('bg-primary-subtle');
                // Xóa badge unread trên UI
                const badge = b.querySelector('.badge');
                if (badge) badge.remove();
            } else {
                b.classList.remove('bg-primary-subtle');
            }
        });

        // Hiện vùng chat
        noSelectedPlaceholder.classList.add('d-none');
        conversationWrapper.classList.remove('d-none');
        conversationWrapper.style.display = 'flex';

        // Điền header
        const initial = (room.customerName || 'K').charAt(0).toUpperCase();
        if (room.customerAvatar) {
            activeAvatar.innerHTML = `<img src="${escapeHtml(room.customerAvatar)}" alt="">`;
        } else {
            activeAvatar.textContent = initial;
        }
        activeName.textContent = room.customerName || 'Khách hàng';
        activeEmail.textContent = room.customerEmail || '';

        // Nút tiếp nhận
        if (!room.employeeId) {
            btnAssignMe.classList.remove('d-none');
        } else {
            btnAssignMe.classList.add('d-none');
        }

        // Tải tin nhắn
        await loadMessages(roomId);

        // Đăng ký channel WebSocket của room này
        subscribeToRoom(roomId);

        if (chatInput) chatInput.focus();
    }

    const renderedMsgIds = new Set();

    // 3. Tải tin nhắn của phòng
    async function loadMessages(roomId) {
        messagesStream.innerHTML = '<div class="text-center py-4 text-muted small">Đang tải tin nhắn...</div>';
        renderedMsgIds.clear();
        try {
            const res = await fetch(`/api/employee/chat/room/${roomId}/messages`);
            const resData = await res.json();
            if (!resData.success || !resData.data) return;

            messagesStream.innerHTML = `
                <div class="chat-msg chat-msg-system">
                    Bắt đầu phiên tư vấn cho khách hàng ${escapeHtml(activeName.textContent)}.
                </div>
            `;

            resData.data.forEach(appendMessage);
            scrollToBottom();
        } catch (err) {
            console.error('Load messages error:', err);
        }
    }

    function appendMessage(msg) {
        if (!messagesStream || !msg) return;

        if (msg.id) {
            const strId = String(msg.id);
            if (renderedMsgIds.has(strId)) return;
            renderedMsgIds.add(strId);
        }

        // Nếu tin nhắn do NHÂN VIÊN gửi thì là tin bên phải (sent), nếu do KHÁCH HÀNG gửi là bên trái (received)
        const isEmployee = (msg.senderRole || '').toUpperCase().includes('EMPLOYEE') || (msg.senderRole || '').toUpperCase().includes('MANAGER');

        const isImage = msg.messageType === 'IMAGE';
        const div = document.createElement('div');
        div.className = `chat-msg ${isImage ? 'chat-msg-image' : ''} ${isEmployee ? 'chat-msg-sent' : 'chat-msg-received'}`;

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

        const timeStr = formatTime(msg.createdAt);
        div.innerHTML = `
            ${!isEmployee && msg.senderName ? `<div class="fw-bold small text-primary mb-1" style="font-size: 0.72rem;">${escapeHtml(msg.senderName)}</div>` : ''}
            ${contentHtml}
            <div class="chat-msg-time">${timeStr}</div>
        `;

        messagesStream.appendChild(div);
    }

    function appendSystemMessage(text) {
        if (!messagesStream) return;
        const div = document.createElement('div');
        div.className = 'chat-msg chat-msg-system';
        div.textContent = text;
        messagesStream.appendChild(div);
    }

    function scrollToBottom() {
        if (messagesStream) {
            messagesStream.scrollTop = messagesStream.scrollHeight;
        }
    }

    // 4. WebSocket kết nối
    function initWebSocket() {
        try {
            stompClient = new StompJs.Client({
                webSocketFactory: () => new SockJS('/ws'),
                reconnectDelay: 5000
            });

            stompClient.onConnect = function () {
                isConnected = true;

                // Sub thông báo tin nhắn mới toàn cục cho nhân viên
                stompClient.subscribe('/topic/employee/chat-notifications', function (msgOutput) {
                    try {
                        const data = JSON.parse(msgOutput.body);
                        if (data.type === 'NEW_MESSAGE') {
                            if (typeof showToast === 'function') {
                                showToast('Tin nhắn mới!', `${data.customerName}: ${data.content}`, 'info');
                            }
                            // Cập nhật lại danh sách phòng
                            refreshRoomsSilent();
                        }
                    } catch (e) {
                        console.error('Parse WS notification error:', e);
                    }
                });

                if (currentRoomId) {
                    subscribeToRoom(currentRoomId);
                }
            };

            stompClient.activate();
        } catch (e) {
            console.warn('WS error:', e);
        }
    }

    function subscribeToRoom(roomId) {
        if (!stompClient || !isConnected) return;
        if (activeRoomSubscription) {
            activeRoomSubscription.unsubscribe();
        }

        activeRoomSubscription = stompClient.subscribe('/topic/chat/' + roomId, function (msgOutput) {
            try {
                const data = JSON.parse(msgOutput.body);
                if (data.type === 'EMPLOYEE_JOINED' || data.type === 'ROOM_CLOSED') {
                    appendSystemMessage(data.message);
                } else {
                    appendMessage(data);
                }
                scrollToBottom();
            } catch (e) {
                console.error('Parse room msg error:', e);
            }
        });
    }

    async function refreshRoomsSilent() {
        try {
            const res = await fetch('/api/employee/chat/rooms');
            const resData = await res.json();
            if (resData.success && resData.data) {
                allRooms = resData.data;
                updateSidebarBadge(allRooms.length);
                renderRoomsList(allRooms);
            }
        } catch (e) {}
    }

    // 5. Gửi tin nhắn
    async function doSendMessage() {
        if (!chatInput) return;
        const text = chatInput.value.trim();
        if (!text) return;

        if (!currentRoomId) {
            alert('Vui lòng chọn hoặc mở một cuộc trò chuyện để gửi tin nhắn.');
            return;
        }

        if (btnSend) btnSend.disabled = true;

        try {
            const res = await fetch('/api/employee/chat/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({
                    roomId: currentRoomId,
                    content: text,
                    messageType: 'TEXT'
                })
            });

            const resData = await res.json();
            if (res.ok && resData.success && resData.data) {
                chatInput.value = '';
                appendMessage(resData.data);
                scrollToBottom();

                // Cập nhật nội dung tin nhắn mới nhất trong danh sách phòng
                const roomItem = allRooms.find(r => String(r.id) === String(currentRoomId));
                if (roomItem) {
                    roomItem.lastMessage = text;
                    roomItem.lastMessageTime = new Date().toISOString();
                    renderRoomsList(allRooms);
                }
            } else {
                alert(resData.message || 'Lỗi khi gửi tin nhắn. Vui lòng thử lại.');
            }
        } catch (err) {
            console.error('Send message error:', err);
            alert('Lỗi kết nối khi gửi tin nhắn.');
        } finally {
            if (btnSend) btnSend.disabled = false;
            if (chatInput) chatInput.focus();
        }
    }

    if (btnSend) btnSend.addEventListener('click', doSendMessage);
    if (chatInput) {
        chatInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                doSendMessage();
            }
        });
    }

    // 6. Gửi hình ảnh
    if (btnUploadImg && fileInput) {
        btnUploadImg.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', async () => {
            const file = fileInput.files && fileInput.files[0];
            if (!file || !currentRoomId) return;

            const formData = new FormData();
            formData.append('file', file);

            try {
                btnUploadImg.disabled = true;
                const uploadRes = await fetch('/api/employee/chat/upload-image', {
                    method: 'POST',
                    body: formData
                });
                const uploadData = await uploadRes.json();
                if (uploadData.success && uploadData.data) {
                    const sendRes = await fetch('/api/employee/chat/message', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json'
                        },
                        body: JSON.stringify({
                            roomId: currentRoomId,
                            content: uploadData.data,
                            messageType: 'IMAGE'
                        })
                    });
                    const sendData = await sendRes.json();
                    if (sendRes.ok && sendData.success && sendData.data) {
                        appendMessage(sendData.data);
                        scrollToBottom();
                    }
                } else {
                    alert('Tải ảnh thất bại: ' + (uploadData.message || ''));
                }
            } catch (e) {
                console.error('Upload error:', e);
                alert('Lỗi kết nối khi tải ảnh.');
            } finally {
                btnUploadImg.disabled = false;
                fileInput.value = '';
            }
        });
    }

    // 7. Tiếp nhận hỗ trợ
    if (btnAssignMe) {
        btnAssignMe.addEventListener('click', async () => {
            if (!currentRoomId) return;
            try {
                btnAssignMe.disabled = true;
                const res = await fetch(`/api/employee/chat/room/${currentRoomId}/assign`, {
                    method: 'PUT'
                });
                const resData = await res.json();
                if (resData.success) {
                    btnAssignMe.classList.add('d-none');
                    if (typeof showToast === 'function') {
                        showToast('Thành công', 'Bạn đã tiếp nhận cuộc trò chuyện này!', 'success');
                    }
                }
            } catch (err) {
                console.error('Assign error:', err);
            } finally {
                btnAssignMe.disabled = false;
            }
        });
    }

    // 8. Đóng cuộc trò chuyện
    if (btnCloseRoom) {
        btnCloseRoom.addEventListener('click', async () => {
            if (!currentRoomId) return;
            if (!confirm('Bạn có chắc chắn muốn kết thúc cuộc hỗ trợ này?')) return;

            try {
                btnCloseRoom.disabled = true;
                const res = await fetch(`/api/employee/chat/room/${currentRoomId}/close`, {
                    method: 'PUT'
                });
                const resData = await res.json();
                if (resData.success) {
                    if (typeof showToast === 'function') {
                        showToast('Đã kết thúc', 'Cuộc trò chuyện đã kết thúc.', 'info');
                    }
                    currentRoomId = null;
                    conversationWrapper.classList.add('d-none');
                    noSelectedPlaceholder.classList.remove('d-none');
                    loadRooms();
                }
            } catch (err) {
                console.error('Close room error:', err);
            } finally {
                btnCloseRoom.disabled = false;
            }
        });
    }

    // 9. Tìm kiếm khách hàng
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            const query = searchInput.value.toLowerCase().trim();
            if (!query) {
                renderRoomsList(allRooms);
            } else {
                const filtered = allRooms.filter(r =>
                    (r.customerName || '').toLowerCase().includes(query) ||
                    (r.customerEmail || '').toLowerCase().includes(query)
                );
                renderRoomsList(filtered);
            }
        });
    }

    if (btnRefresh) {
        btnRefresh.addEventListener('click', loadRooms);
    }

    function formatTime(dateStr) {
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

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    async function checkUrlParamsAndOpenRoom() {
        const urlParams = new URLSearchParams(window.location.search);
        const customerId = urlParams.get('customerId');
        const roomId = urlParams.get('roomId');

        if (roomId) {
            selectRoom(roomId);
            return;
        }

        if (customerId) {
            try {
                const res = await fetch(`/api/employee/chat/room-by-customer/${customerId}`);
                const resData = await res.json();
                if (resData.success && resData.data) {
                    const room = resData.data;
                    const exists = allRooms.some(r => String(r.id) === String(room.id));
                    if (!exists) {
                        allRooms.unshift(room);
                        renderRoomsList(allRooms);
                    }
                    selectRoom(room.id);
                }
            } catch (err) {
                console.error('Error opening room by customerId:', err);
            }
        }
    }

    // Khởi động
    async function init() {
        initWebSocket();
        await loadRooms();
        await checkUrlParamsAndOpenRoom();
    }
    init();
});
