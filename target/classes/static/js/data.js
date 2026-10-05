/* ==========================================================================
   TechNova Electronics — Product Catalogue (data layer)
   --------------------------------------------------------------------------
   This is the single source of truth for every product on the site. All pages
   (home, products, product details, cart, wishlist) read from this array, so
   adding a new product here makes it appear everywhere automatically.

   Each product object:
     id          unique number, used as the key in cart/wishlist storage
     name        display title
     brand       manufacturer shown on the card and details page
     category    must match one of the CATEGORIES ids below
     price       current selling price in INR
     oldPrice    struck-through MRP (used to calculate the discount badge)
     rating      average star rating 0-5
     reviewCount number of ratings, shown next to the stars
     badge       optional ribbon on the card ("Best Seller", "New", ...)
     stock       units available; 0 renders an "Out of Stock" state
     featured    true => shown in the "Featured Products" home section
     bestSeller  true => shown in the "Best Sellers" home section
     tagline     one-line summary used on cards and meta descriptions
     description longer marketing copy for the details page
     highlights  bullet list of key selling points
     specs       key/value table rendered in the Specifications tab
     images      gallery images, first one is the main/card image
     reviews     customer reviews rendered in the Reviews tab
   ========================================================================== */

/* Category definitions — drive the nav dropdown, filters and home tiles. */
/* Base URL of the images folder. window.APP_URL (= ${URL}) is declared in fragments/header.html */
const IMG_BASE = (window.APP_URL || '/') + 'images/';

const CATEGORIES = [
  { id: 'laptops',      name: 'Laptops',           icon: 'fa-laptop',        image: IMG_BASE + 'cat-laptops.svg',      blurb: 'Ultrabooks, creator rigs & gaming beasts' },
  { id: 'smartphones',  name: 'Smartphones',       icon: 'fa-mobile-screen', image: IMG_BASE + 'cat-smartphones.svg',  blurb: 'Flagship cameras and all-day 5G battery' },
  { id: 'headphones',   name: 'Headphones',        icon: 'fa-headphones',    image: IMG_BASE + 'cat-headphones.svg',   blurb: 'Studio sound with active noise cancelling' },
  { id: 'smartwatches', name: 'Smartwatches',      icon: 'fa-clock',         image: IMG_BASE + 'cat-smartwatches.svg', blurb: 'Fitness, health and notifications on wrist' },
  { id: 'gaming',       name: 'Gaming Accessories', icon: 'fa-gamepad',      image: IMG_BASE + 'cat-gaming.svg',       blurb: 'Mechanical keys, low-latency gear & RGB' }
];

const PRODUCTS = [
  /* ----------------------------------------------------------- LAPTOPS -- */
  {
    id: 1,
    name: 'NovaBook Pro 16',
    brand: 'TechNova',
    category: 'laptops',
    price: 184999,
    oldPrice: 209999,
    rating: 4.8,
    reviewCount: 412,
    badge: 'Best Seller',
    stock: 12,
    featured: true,
    bestSeller: true,
    tagline: '16" Retina-class display, 32GB RAM, 1TB SSD creator powerhouse.',
    description:
      'The NovaBook Pro 16 is built for people who render, compile and edit for a living. A 16-inch Liquid Mini-LED panel covers 100% DCI-P3, while the NovaSilicon M-series chip pushes 22 hours of real-world battery life without ever spinning up a fan during everyday work.',
    highlights: [
      '16.2" Mini-LED display, 120Hz ProMotion, 1600 nits peak',
      '12-core CPU + 30-core GPU NovaSilicon chip',
      '32GB unified memory and blazing 1TB NVMe storage',
      'Up to 22 hours battery with 96W fast charging',
      'Six-speaker sound system with spatial audio'
    ],
    specs: {
      'Display': '16.2-inch Mini-LED, 3456 x 2234, 120Hz',
      'Processor': 'NovaSilicon M5 Pro, 12-core',
      'Graphics': '30-core integrated GPU',
      'Memory': '32GB LPDDR5 unified',
      'Storage': '1TB NVMe SSD',
      'Battery': '100Wh, up to 22 hours video playback',
      'Ports': '3x Thunderbolt 5, HDMI 2.1, SDXC, MagLock',
      'Weight': '2.15 kg',
      'Operating System': 'NovaOS 15',
      'Warranty': '1 Year Onsite Manufacturer Warranty'
    },
    images: [IMG_BASE + 'novabook-pro-16-1.svg', IMG_BASE + 'novabook-pro-16-2.svg', IMG_BASE + 'novabook-pro-16-3.svg'],
    reviews: [
      { name: 'Arjun Mehta', rating: 5, date: '12 June 2026', title: 'Replaced my desktop entirely', text: 'I edit 4K footage daily and this machine does not even get warm. Battery genuinely lasts a full working day.' },
      { name: 'Priya Nair', rating: 5, date: '28 May 2026', title: 'The display is unreal', text: 'Colours are accurate straight out of the box. Worth every rupee for design work.' },
      { name: 'Rohit Sharma', rating: 4, date: '03 May 2026', title: 'Excellent but heavy', text: 'Performance is outstanding. Slightly heavy to carry around campus all day, but that is the trade-off.' }
    ]
  },
  {
    id: 2,
    name: 'NovaBook Air 13',
    brand: 'TechNova',
    category: 'laptops',
    price: 89999,
    oldPrice: 104999,
    rating: 4.6,
    reviewCount: 638,
    badge: 'Student Pick',
    stock: 34,
    featured: true,
    bestSeller: true,
    tagline: 'Fanless 1.1kg ultrabook that runs cool and silent all day.',
    description:
      'At just 1.1kg the NovaBook Air 13 disappears into a backpack, yet it handles coding, research and streaming without a single fan. The fanless chassis means it is completely silent, and the 18-hour battery removes the charger from your daily carry.',
    highlights: [
      '13.6" Liquid Retina display with True Tone',
      'Completely fanless — zero noise under load',
      '16GB RAM, 512GB SSD configuration',
      '18-hour battery life, charges over USB-C',
      'Backlit keyboard with large glass trackpad'
    ],
    specs: {
      'Display': '13.6-inch IPS, 2560 x 1664, 60Hz',
      'Processor': 'NovaSilicon M5, 8-core',
      'Graphics': '10-core integrated GPU',
      'Memory': '16GB LPDDR5 unified',
      'Storage': '512GB NVMe SSD',
      'Battery': '52.6Wh, up to 18 hours',
      'Ports': '2x Thunderbolt 4, 3.5mm audio',
      'Weight': '1.10 kg',
      'Operating System': 'NovaOS 15',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'novabook-air-13-1.svg', IMG_BASE + 'novabook-air-13-2.svg', IMG_BASE + 'novabook-air-13-3.svg'],
    reviews: [
      { name: 'Simran Kaur', rating: 5, date: '19 June 2026', title: 'Perfect college laptop', text: 'Light enough that I forget it is in my bag and the battery survives a full day of lectures.' },
      { name: 'Devansh Gupta', rating: 4, date: '07 June 2026', title: 'Great, wish it had more ports', text: 'Fast and silent. Only two USB-C ports though, so budget for a hub.' }
    ]
  },
  {
    id: 3,
    name: 'Zenith Gaming Laptop X17',
    brand: 'Zenith',
    category: 'laptops',
    price: 219999,
    oldPrice: 249999,
    rating: 4.7,
    reviewCount: 287,
    badge: 'RTX Powered',
    stock: 8,
    featured: true,
    bestSeller: false,
    tagline: '17" 240Hz QHD, RTX 5080 and vapour-chamber cooling.',
    description:
      'The Zenith X17 is a desktop replacement in every sense. A 240Hz QHD panel and RTX 5080 graphics push competitive frame rates, while the dual vapour chamber keeps sustained clocks high through long sessions. Per-key RGB and a mechanical-feel keyboard round out the package.',
    highlights: [
      '17.3" QHD 240Hz panel with G-Sync',
      'NVIDIA RTX 5080 Laptop GPU, 175W TGP',
      'Intel Core i9 HX processor, 24 cores',
      '32GB DDR5-5600 and 2TB Gen4 SSD',
      'Dual vapour-chamber cooling with liquid metal'
    ],
    specs: {
      'Display': '17.3-inch IPS, 2560 x 1440, 240Hz',
      'Processor': 'Intel Core i9-14900HX',
      'Graphics': 'NVIDIA GeForce RTX 5080 16GB',
      'Memory': '32GB DDR5-5600 (upgradeable to 64GB)',
      'Storage': '2TB PCIe Gen4 NVMe SSD',
      'Battery': '99.9Wh with 330W adapter',
      'Ports': '2x USB-C, 3x USB-A, HDMI 2.1, RJ45',
      'Weight': '2.95 kg',
      'Operating System': 'Windows 11 Home',
      'Warranty': '2 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'zenith-gaming-x17-1.svg', IMG_BASE + 'zenith-gaming-x17-2.svg', IMG_BASE + 'zenith-gaming-x17-3.svg'],
    reviews: [
      { name: 'Karan Malhotra', rating: 5, date: '22 June 2026', title: 'Frames for days', text: 'Runs everything maxed at QHD. Fans get loud in turbo mode but the temps stay excellent.' },
      { name: 'Ananya Rao', rating: 4, date: '11 May 2026', title: 'Beast, but not portable', text: 'Incredible performance for the price. Just know that the charger alone weighs a kilo.' }
    ]
  },
  {
    id: 4,
    name: 'Creator Studio 15',
    brand: 'Zenith',
    category: 'laptops',
    price: 149999,
    oldPrice: 169999,
    rating: 4.5,
    reviewCount: 194,
    badge: null,
    stock: 15,
    featured: false,
    bestSeller: false,
    tagline: 'Colour-calibrated OLED laptop tuned for designers and editors.',
    description:
      'Every Creator Studio 15 ships with an individually calibrated 4K OLED panel and a Delta-E under 1, so what you design is what your client sees. A dedicated hardware encoder cuts export times roughly in half compared with software-only rendering.',
    highlights: [
      '15.6" 4K OLED, 100% Adobe RGB, factory calibrated',
      'Hardware media encoder for fast video exports',
      'RTX 5060 graphics with Studio drivers',
      '32GB RAM and 1TB SSD, both upgradeable',
      'Full-size SD card reader built in'
    ],
    specs: {
      'Display': '15.6-inch OLED, 3840 x 2160, 60Hz, Delta-E < 1',
      'Processor': 'Intel Core Ultra 9 185H',
      'Graphics': 'NVIDIA GeForce RTX 5060 8GB',
      'Memory': '32GB DDR5-5200',
      'Storage': '1TB PCIe Gen4 NVMe SSD',
      'Battery': '90Wh, up to 11 hours',
      'Ports': '2x Thunderbolt 4, 2x USB-A, HDMI, SD reader',
      'Weight': '1.80 kg',
      'Operating System': 'Windows 11 Pro',
      'Warranty': '1 Year Onsite Warranty'
    },
    images: [IMG_BASE + 'creator-studio-15-1.svg', IMG_BASE + 'creator-studio-15-2.svg', IMG_BASE + 'creator-studio-15-3.svg'],
    reviews: [
      { name: 'Meera Iyer', rating: 5, date: '02 June 2026', title: 'Colour accuracy is spot on', text: 'Calibration report in the box matched my own measurements. Rare at this price.' }
    ]
  },
  {
    id: 5,
    name: 'NovaBook Lite 14',
    brand: 'TechNova',
    category: 'laptops',
    price: 44999,
    oldPrice: 54999,
    rating: 4.2,
    reviewCount: 856,
    badge: 'Budget King',
    stock: 62,
    featured: false,
    bestSeller: true,
    tagline: 'Everyday computing essentials at an unbeatable student price.',
    description:
      'Assignments, browser tabs, video calls and streaming — the NovaBook Lite 14 handles the everyday without drama. A full-HD anti-glare screen and 10-hour battery make it an easy first laptop, and the SSD keeps boot times under ten seconds.',
    highlights: [
      '14" Full HD anti-glare display',
      'Ryzen 5 processor with integrated graphics',
      '8GB RAM expandable to 16GB',
      '512GB SSD for fast boot and load times',
      'Fingerprint reader and HD webcam with shutter'
    ],
    specs: {
      'Display': '14-inch IPS, 1920 x 1080, anti-glare',
      'Processor': 'AMD Ryzen 5 7530U',
      'Graphics': 'AMD Radeon Integrated',
      'Memory': '8GB DDR4 (1 slot free)',
      'Storage': '512GB NVMe SSD',
      'Battery': '54Wh, up to 10 hours',
      'Ports': '1x USB-C, 2x USB-A, HDMI, 3.5mm',
      'Weight': '1.45 kg',
      'Operating System': 'Windows 11 Home',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'novabook-lite-14-1.svg', IMG_BASE + 'novabook-lite-14-2.svg', IMG_BASE + 'novabook-lite-14-3.svg'],
    reviews: [
      { name: 'Vikram Singh', rating: 4, date: '15 June 2026', title: 'Great value for money', text: 'Does everything a first-year student needs. Added 8GB RAM myself and it flies.' },
      { name: 'Nisha Verma', rating: 4, date: '30 April 2026', title: 'Solid budget pick', text: 'Screen could be brighter but for the price I cannot complain at all.' }
    ]
  },

  /* ------------------------------------------------------ SMARTPHONES -- */
  {
    id: 6,
    name: 'Nova X5 Pro 5G',
    brand: 'TechNova',
    category: 'smartphones',
    price: 79999,
    oldPrice: 94999,
    rating: 4.7,
    reviewCount: 1247,
    badge: 'Best Seller',
    stock: 41,
    featured: true,
    bestSeller: true,
    tagline: '200MP periscope camera, titanium frame and 120Hz LTPO AMOLED.',
    description:
      'The Nova X5 Pro pairs a 200MP main sensor with a 5x periscope telephoto, so distant subjects stay sharp without digital mush. The LTPO panel drops to 1Hz when you are reading and ramps to 120Hz when you scroll, which is how it stretches a 5000mAh cell across two days.',
    highlights: [
      '6.8" LTPO AMOLED, 1-120Hz adaptive, 2600 nits',
      '200MP main + 50MP ultrawide + 5x periscope',
      'Titanium frame with Gorilla Glass Victus 3',
      '5000mAh battery with 100W wired, 50W wireless',
      'IP68 water and dust resistance'
    ],
    specs: {
      'Display': '6.8-inch LTPO AMOLED, 3120 x 1440, 120Hz',
      'Processor': 'Snapdragon 8 Gen 4',
      'Rear Camera': '200MP OIS + 50MP ultrawide + 64MP 5x periscope',
      'Front Camera': '32MP autofocus',
      'Memory': '12GB RAM',
      'Storage': '256GB UFS 4.0',
      'Battery': '5000mAh, 100W wired charging',
      'Protection': 'IP68 rated',
      'Operating System': 'NovaUI 8 (Android 16)',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'nova-x5-pro-1.svg', IMG_BASE + 'nova-x5-pro-2.svg', IMG_BASE + 'nova-x5-pro-3.svg'],
    reviews: [
      { name: 'Aditya Kulkarni', rating: 5, date: '25 June 2026', title: 'Camera is genuinely flagship', text: 'The 5x zoom is usable in low light, which is rare. Night mode shots need no editing.' },
      { name: 'Sanya Kapoor', rating: 5, date: '14 June 2026', title: 'Two days on one charge', text: 'Coming from an older phone this battery life feels like magic. 100W charging is instant.' },
      { name: 'Harsh Patel', rating: 4, date: '01 June 2026', title: 'Superb but slippery', text: 'Titanium looks premium but definitely get a case. Performance is flawless.' }
    ]
  },
  {
    id: 7,
    name: 'Nova X5 Lite 5G',
    brand: 'TechNova',
    category: 'smartphones',
    price: 24999,
    oldPrice: 31999,
    rating: 4.4,
    reviewCount: 2140,
    badge: 'Value Pick',
    stock: 88,
    featured: false,
    bestSeller: true,
    tagline: 'Flagship-style 120Hz AMOLED at a genuinely mid-range price.',
    description:
      'The X5 Lite borrows the design language of its Pro sibling and keeps the things that matter day to day: a 120Hz AMOLED, clean software with no bloatware, and a 5000mAh battery that comfortably clears a full day of heavy use.',
    highlights: [
      '6.6" AMOLED with 120Hz refresh rate',
      '64MP OIS main camera with night mode',
      '8GB RAM + 128GB expandable storage',
      '5000mAh battery with 67W fast charging',
      'Clean NovaUI with three years of OS updates'
    ],
    specs: {
      'Display': '6.6-inch AMOLED, 2400 x 1080, 120Hz',
      'Processor': 'Snapdragon 7s Gen 3',
      'Rear Camera': '64MP OIS + 8MP ultrawide + 2MP macro',
      'Front Camera': '16MP',
      'Memory': '8GB RAM',
      'Storage': '128GB (microSD up to 1TB)',
      'Battery': '5000mAh, 67W fast charging',
      'Protection': 'IP54 splash resistant',
      'Operating System': 'NovaUI 8 (Android 16)',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'nova-x5-lite-1.svg', IMG_BASE + 'nova-x5-lite-2.svg', IMG_BASE + 'nova-x5-lite-3.svg'],
    reviews: [
      { name: 'Ishaan Joshi', rating: 5, date: '20 June 2026', title: 'Best phone under 25k', text: 'No ads, no bloatware, smooth 120Hz screen. Nothing else at this price does all three.' },
      { name: 'Tanvi Desai', rating: 4, date: '08 May 2026', title: 'Good all-rounder', text: 'Ultrawide camera is average but the main sensor is genuinely good.' }
    ]
  },
  {
    id: 8,
    name: 'Pulse Ultra 12',
    brand: 'Pulse',
    category: 'smartphones',
    price: 109999,
    oldPrice: 124999,
    rating: 4.8,
    reviewCount: 673,
    badge: 'Premium',
    stock: 19,
    featured: true,
    bestSeller: false,
    tagline: 'Pro-grade 1-inch sensor with a co-engineered optical lens.',
    description:
      'A full 1-inch type sensor sits behind the Pulse Ultra 12\'s main camera, gathering roughly twice the light of a typical flagship. Combine that with a variable f/1.6-f/4.0 aperture and you get real depth-of-field control instead of software approximations.',
    highlights: [
      '1-inch type 50MP sensor with variable aperture',
      '6.73" LTPO AMOLED, 3000 nits peak brightness',
      'Snapdragon 8 Gen 4 with vapour chamber',
      '16GB RAM and 512GB UFS 4.0 storage',
      '90W wired and 80W wireless charging'
    ],
    specs: {
      'Display': '6.73-inch LTPO AMOLED, 3200 x 1440, 120Hz',
      'Processor': 'Snapdragon 8 Gen 4',
      'Rear Camera': '50MP 1-inch + 50MP ultrawide + 50MP 3.2x tele',
      'Front Camera': '32MP',
      'Memory': '16GB LPDDR5X',
      'Storage': '512GB UFS 4.0',
      'Battery': '5400mAh, 90W wired / 80W wireless',
      'Protection': 'IP68 rated',
      'Operating System': 'PulseOS 6 (Android 16)',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'pulse-ultra-12-1.svg', IMG_BASE + 'pulse-ultra-12-2.svg', IMG_BASE + 'pulse-ultra-12-3.svg'],
    reviews: [
      { name: 'Rahul Bansal', rating: 5, date: '18 June 2026', title: 'Best camera phone, period', text: 'The 1-inch sensor makes a visible difference indoors. Bokeh is optical, not fake.' },
      { name: 'Kavya Menon', rating: 5, date: '05 June 2026', title: 'Worth the premium', text: 'Expensive but nothing feels compromised anywhere. Screen brightness outdoors is incredible.' }
    ]
  },
  {
    id: 9,
    name: 'Orbit Fold 3',
    brand: 'Orbit',
    category: 'smartphones',
    price: 159999,
    oldPrice: 179999,
    rating: 4.3,
    reviewCount: 218,
    badge: 'New Launch',
    stock: 7,
    featured: true,
    bestSeller: false,
    tagline: 'A 7.6" tablet that folds into a pocketable phone.',
    description:
      'The Orbit Fold 3 opens into a 7.6-inch canvas for split-screen multitasking, then folds shut into a phone that fits a jeans pocket. A redesigned water-drop hinge closes completely flat and has cut the display crease down to nearly invisible.',
    highlights: [
      '7.6" foldable AMOLED main + 6.2" cover display',
      'Gapless water-drop hinge rated for 400,000 folds',
      'Three-app split screen with drag and drop',
      '12GB RAM and 512GB storage',
      'S-Pen support on the inner display'
    ],
    specs: {
      'Main Display': '7.6-inch Foldable AMOLED, 120Hz',
      'Cover Display': '6.2-inch AMOLED, 120Hz',
      'Processor': 'Snapdragon 8 Gen 4 for Orbit',
      'Rear Camera': '50MP + 12MP ultrawide + 10MP 3x tele',
      'Memory': '12GB RAM',
      'Storage': '512GB UFS 4.0',
      'Battery': '4600mAh, 45W charging',
      'Protection': 'IPX8 water resistant',
      'Operating System': 'OrbitOS 7 (Android 16)',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'orbit-fold-3-1.svg', IMG_BASE + 'orbit-fold-3-2.svg', IMG_BASE + 'orbit-fold-3-3.svg'],
    reviews: [
      { name: 'Nikhil Chawla', rating: 4, date: '21 June 2026', title: 'Multitasking changed my workflow', text: 'Reading PDFs and taking notes side by side is brilliant. Battery is the only weak spot.' }
    ]
  },
  {
    id: 10,
    name: 'Nova SE 2026',
    brand: 'TechNova',
    category: 'smartphones',
    price: 18999,
    oldPrice: 22999,
    rating: 3.9,
    reviewCount: 3021,
    badge: null,
    stock: 120,
    featured: false,
    bestSeller: false,
    tagline: 'Compact, light and dependable — the everyday essential.',
    description:
      'Not everyone wants a slab the size of a paperback. The Nova SE 2026 keeps a comfortable 6.1-inch footprint, runs the same clean software as its bigger siblings, and delivers reliable battery life for calls, chat, maps and music.',
    highlights: [
      '6.1" compact display, easy one-handed use',
      '50MP main camera with electronic stabilisation',
      '6GB RAM and 128GB storage',
      '5000mAh battery, up to 2 days moderate use',
      'Side fingerprint sensor and 3.5mm headphone jack'
    ],
    specs: {
      'Display': '6.1-inch IPS LCD, 2340 x 1080, 90Hz',
      'Processor': 'Dimensity 7050',
      'Rear Camera': '50MP + 2MP depth',
      'Front Camera': '8MP',
      'Memory': '6GB RAM',
      'Storage': '128GB (microSD supported)',
      'Battery': '5000mAh, 33W charging',
      'Protection': 'IP52',
      'Operating System': 'NovaUI 8 (Android 16)',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'nova-se-2026-1.svg', IMG_BASE + 'nova-se-2026-2.svg', IMG_BASE + 'nova-se-2026-3.svg'],
    reviews: [
      { name: 'Pooja Reddy', rating: 4, date: '10 June 2026', title: 'Finally a small phone', text: 'Fits my hand and my pocket. Does everything I need without costing a fortune.' },
      { name: 'Manish Tiwari', rating: 4, date: '27 May 2026', title: 'Reliable daily driver', text: 'Bought for my parents. Simple, fast enough, and the battery lasts them three days.' }
    ]
  },

  /* ------------------------------------------------------- HEADPHONES -- */
  {
    id: 11,
    name: 'SonicWave Pro ANC',
    brand: 'SonicWave',
    category: 'headphones',
    price: 24999,
    oldPrice: 32999,
    rating: 4.9,
    reviewCount: 1583,
    badge: 'Editor\'s Choice',
    stock: 46,
    featured: true,
    bestSeller: true,
    tagline: 'Class-leading noise cancelling with 40-hour battery life.',
    description:
      'Eight microphones sample ambient noise 600 times a second, which is why the SonicWave Pro cancels engine drone and office chatter so convincingly. Memory-foam earcups wrapped in protein leather stay comfortable well past the four-hour mark.',
    highlights: [
      'Adaptive ANC with eight-microphone array',
      '40 hours playback, 60 hours with ANC off',
      'Hi-Res certified 40mm dynamic drivers',
      'Multipoint pairing across two devices',
      '5-minute quick charge gives 5 hours of listening'
    ],
    specs: {
      'Driver': '40mm dynamic, Hi-Res Audio certified',
      'Noise Cancelling': 'Adaptive Hybrid ANC, 8 microphones',
      'Battery': '40 hours (ANC on), 60 hours (ANC off)',
      'Charging': 'USB-C, 5 min = 5 hours playback',
      'Bluetooth': 'v5.4 with LDAC and aptX Adaptive',
      'Multipoint': 'Yes, two devices simultaneously',
      'Weight': '254 g',
      'Controls': 'Touch panel + physical buttons',
      'In the Box': 'Headphones, hard case, 3.5mm cable, USB-C cable',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'sonicwave-pro-anc-1.svg', IMG_BASE + 'sonicwave-pro-anc-2.svg', IMG_BASE + 'sonicwave-pro-anc-3.svg'],
    reviews: [
      { name: 'Aarav Krishnan', rating: 5, date: '24 June 2026', title: 'Flight noise simply disappears', text: 'Used these on a six-hour flight and barely heard the engines. Comfort is excellent too.' },
      { name: 'Riya Sen', rating: 5, date: '16 June 2026', title: 'Battery is ridiculous', text: 'I charge them roughly once every two weeks. Sound is warm without being muddy.' },
      { name: 'Zoya Khan', rating: 5, date: '02 June 2026', title: 'Worth every rupee', text: 'Compared these against two other flagships in store and these won on comfort easily.' }
    ]
  },
  {
    id: 12,
    name: 'AeroBuds 3 Pro',
    brand: 'SonicWave',
    category: 'headphones',
    price: 14999,
    oldPrice: 19999,
    rating: 4.6,
    reviewCount: 2847,
    badge: 'Best Seller',
    stock: 73,
    featured: true,
    bestSeller: true,
    tagline: 'Tiny true-wireless buds with big adaptive noise cancelling.',
    description:
      'AeroBuds 3 Pro weigh under five grams per bud yet fit a custom driver and dual-chamber vent that gives them genuine bass extension. Adaptive transparency lets conversation through automatically the moment someone speaks to you.',
    highlights: [
      'Adaptive ANC with conversation-aware transparency',
      '9 hours per charge, 32 hours with the case',
      'Custom 11mm drivers with spatial audio',
      'IPX5 sweat and splash resistant',
      'Wireless charging case with finder chime'
    ],
    specs: {
      'Driver': '11mm custom dynamic',
      'Noise Cancelling': 'Adaptive ANC + Transparency mode',
      'Battery': '9 hours per bud, 32 hours total with case',
      'Charging': 'USB-C and Qi wireless',
      'Bluetooth': 'v5.4 with LE Audio',
      'Water Resistance': 'IPX5',
      'Weight': '4.9 g per bud',
      'Controls': 'Force-sensor stem controls',
      'In the Box': 'Buds, charging case, 3 ear-tip sizes, USB-C cable',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'aerobuds-3-pro-1.svg', IMG_BASE + 'aerobuds-3-pro-2.svg', IMG_BASE + 'aerobuds-3-pro-3.svg'],
    reviews: [
      { name: 'Dhruv Agarwal', rating: 5, date: '23 June 2026', title: 'Disappear in your ears', text: 'So light I forget I am wearing them. ANC is close to over-ear performance.' },
      { name: 'Aisha Fernandes', rating: 4, date: '12 June 2026', title: 'Great sound, average mic', text: 'Music quality is superb. Call quality is fine indoors but struggles in wind.' }
    ]
  },
  {
    id: 13,
    name: 'BassKing Studio 900',
    brand: 'BassKing',
    category: 'headphones',
    price: 8999,
    oldPrice: 12999,
    rating: 4.3,
    reviewCount: 1129,
    badge: null,
    stock: 55,
    featured: false,
    bestSeller: false,
    tagline: 'Deep, punchy low end tuned for hip-hop and EDM.',
    description:
      'If you want bass you can feel, the Studio 900 delivers. Oversized 50mm drivers and a sealed acoustic chamber push sub-bass hard without drowning the vocals, and the fold-flat design makes them easy to carry.',
    highlights: [
      '50mm bass-tuned drivers with deep sub extension',
      '50 hours of playback per charge',
      'Foldable design with carry pouch included',
      'Built-in mic for calls and voice assistant',
      'Works wired over 3.5mm when the battery dies'
    ],
    specs: {
      'Driver': '50mm dynamic, bass tuned',
      'Frequency Response': '16Hz - 22kHz',
      'Battery': '50 hours playback',
      'Charging': 'USB-C, 2 hours full charge',
      'Bluetooth': 'v5.3',
      'Wired Mode': '3.5mm cable included',
      'Weight': '278 g',
      'Foldable': 'Yes, fold-flat and swivel cups',
      'In the Box': 'Headphones, pouch, 3.5mm cable, USB-C cable',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'bassking-studio-900-1.svg', IMG_BASE + 'bassking-studio-900-2.svg', IMG_BASE + 'bassking-studio-900-3.svg'],
    reviews: [
      { name: 'Yash Thakur', rating: 4, date: '09 June 2026', title: 'Bass heads will love these', text: 'Exactly as advertised. Not neutral at all, but that is the point.' }
    ]
  },
  {
    id: 14,
    name: 'SonicWave Lite',
    brand: 'SonicWave',
    category: 'headphones',
    price: 4999,
    oldPrice: 6999,
    rating: 4.2,
    reviewCount: 1974,
    badge: 'Under 5K',
    stock: 94,
    featured: false,
    bestSeller: true,
    tagline: 'Lightweight everyday over-ears with 60-hour battery.',
    description:
      'The SonicWave Lite strips out the premium extras and keeps the essentials: balanced tuning, comfortable clamping force and a battery that runs for two and a half days of continuous playback.',
    highlights: [
      '40mm drivers with balanced everyday tuning',
      '60 hours of playback on a single charge',
      'Only 190g — comfortable for long sessions',
      'Bluetooth 5.3 with low-latency game mode',
      'Fabric-wrapped headband that resists wear'
    ],
    specs: {
      'Driver': '40mm dynamic',
      'Frequency Response': '20Hz - 20kHz',
      'Battery': '60 hours playback',
      'Charging': 'USB-C',
      'Bluetooth': 'v5.3 with 60ms game mode',
      'Wired Mode': '3.5mm supported',
      'Weight': '190 g',
      'Controls': 'Physical buttons',
      'In the Box': 'Headphones, USB-C cable, quick start guide',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'sonicwave-lite-1.svg', IMG_BASE + 'sonicwave-lite-2.svg', IMG_BASE + 'sonicwave-lite-3.svg'],
    reviews: [
      { name: 'Neha Bhatt', rating: 4, date: '17 June 2026', title: 'Perfect for online classes', text: 'Light on the head for hours and the battery basically never runs out.' },
      { name: 'Sameer Ali', rating: 4, date: '29 May 2026', title: 'Good budget buy', text: 'No ANC at this price obviously, but the sound is clean and balanced.' }
    ]
  },
  {
    id: 15,
    name: 'AeroBuds Air',
    brand: 'SonicWave',
    category: 'headphones',
    price: 2999,
    oldPrice: 4499,
    rating: 3.7,
    reviewCount: 4218,
    badge: null,
    stock: 150,
    featured: false,
    bestSeller: false,
    tagline: 'Affordable true wireless with surprisingly clean sound.',
    description:
      'AeroBuds Air prove that decent true wireless does not need to cost a fortune. Fast pairing, a secure fit and 28 hours of combined battery cover the daily commute, gym session and lecture hall.',
    highlights: [
      '10mm drivers with clear vocal tuning',
      '7 hours per bud, 28 hours with case',
      'Instant pairing the moment you open the case',
      'IPX4 sweat resistant for workouts',
      'Low-latency mode for mobile gaming'
    ],
    specs: {
      'Driver': '10mm dynamic',
      'Battery': '7 hours per bud, 28 hours with case',
      'Charging': 'USB-C',
      'Bluetooth': 'v5.3',
      'Water Resistance': 'IPX4',
      'Weight': '4.2 g per bud',
      'Latency': '68ms game mode',
      'Controls': 'Touch controls',
      'In the Box': 'Buds, case, 3 ear-tip sizes, USB-C cable',
      'Warranty': '6 Months Manufacturer Warranty'
    },
    images: [IMG_BASE + 'aerobuds-air-1.svg', IMG_BASE + 'aerobuds-air-2.svg', IMG_BASE + 'aerobuds-air-3.svg'],
    reviews: [
      { name: 'Kabir Sethi', rating: 4, date: '06 June 2026', title: 'Cannot beat this price', text: 'Bought a pair for the gym. Stay put while running and sound better than expected.' }
    ]
  },

  /* ----------------------------------------------------- SMARTWATCHES -- */
  {
    id: 16,
    name: 'ChronoFit 5 Pro',
    brand: 'ChronoFit',
    category: 'smartwatches',
    price: 34999,
    oldPrice: 41999,
    rating: 4.7,
    reviewCount: 892,
    badge: 'Best Seller',
    stock: 28,
    featured: true,
    bestSeller: true,
    tagline: 'ECG, SpO2 and dual-band GPS in a titanium case.',
    description:
      'The ChronoFit 5 Pro is a proper health instrument on your wrist. Medical-grade ECG, overnight blood-oxygen tracking and skin temperature sensing feed into a recovery score that actually tells you whether to train hard or rest.',
    highlights: [
      '1.9" always-on LTPO AMOLED, 3000 nits',
      'ECG, SpO2, skin temperature and HRV sensors',
      'Dual-band GPS for accurate route tracking',
      'Aerospace-grade titanium case, 100m water rating',
      '7-day battery, 14 days in power-saver mode'
    ],
    specs: {
      'Display': '1.9-inch LTPO AMOLED, always-on, 3000 nits',
      'Case Material': 'Grade 5 Titanium',
      'Health Sensors': 'ECG, SpO2, HRV, skin temperature, heart rate',
      'GPS': 'Dual-band GNSS (L1 + L5)',
      'Battery': 'Up to 7 days typical, 14 days saver mode',
      'Water Resistance': '10 ATM (100m) + IP6X',
      'Connectivity': 'Bluetooth 5.4, Wi-Fi, NFC payments',
      'Compatibility': 'Android 10+ and iOS 16+',
      'Straps': 'Quick-release 22mm, fluoroelastomer included',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'chronofit-5-pro-1.svg', IMG_BASE + 'chronofit-5-pro-2.svg', IMG_BASE + 'chronofit-5-pro-3.svg'],
    reviews: [
      { name: 'Rehan Qureshi', rating: 5, date: '26 June 2026', title: 'Genuinely useful health data', text: 'The recovery score has changed how I plan training weeks. GPS matches my running watch exactly.' },
      { name: 'Divya Prasad', rating: 5, date: '13 June 2026', title: 'Beautiful and tough', text: 'Titanium case has survived two months of gym use without a single scratch.' },
      { name: 'Farhan Sheikh', rating: 4, date: '31 May 2026', title: 'Battery could be better with AOD', text: 'With always-on display I get four days rather than seven. Still excellent though.' }
    ]
  },
  {
    id: 17,
    name: 'ChronoFit SE',
    brand: 'ChronoFit',
    category: 'smartwatches',
    price: 12999,
    oldPrice: 16999,
    rating: 4.4,
    reviewCount: 1653,
    badge: null,
    stock: 67,
    featured: false,
    bestSeller: true,
    tagline: 'The essential ChronoFit experience at half the price.',
    description:
      'The SE keeps the core of what makes a ChronoFit useful — accurate heart rate, sleep staging, 100+ workout modes and smart notifications — in an aluminium case that costs considerably less.',
    highlights: [
      '1.7" AMOLED display with 60Hz refresh',
      '24/7 heart rate and sleep stage tracking',
      'Over 100 workout modes with auto-detect',
      'Built-in GPS, no phone needed on runs',
      '10-day battery life in typical use'
    ],
    specs: {
      'Display': '1.7-inch AMOLED, 390 x 450',
      'Case Material': 'Aluminium alloy',
      'Health Sensors': 'Heart rate, SpO2, sleep tracking',
      'GPS': 'Single-band GPS + GLONASS',
      'Battery': 'Up to 10 days typical use',
      'Water Resistance': '5 ATM (50m)',
      'Connectivity': 'Bluetooth 5.3',
      'Compatibility': 'Android 9+ and iOS 15+',
      'Straps': 'Quick-release 20mm silicone',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'chronofit-se-1.svg', IMG_BASE + 'chronofit-se-2.svg', IMG_BASE + 'chronofit-se-3.svg'],
    reviews: [
      { name: 'Ritika Malhotra', rating: 4, date: '04 June 2026', title: 'Does the job well', text: 'Sleep tracking is accurate and I charge it once a week. Great first smartwatch.' }
    ]
  },
  {
    id: 18,
    name: 'Titan Active GPS',
    brand: 'Titan',
    category: 'smartwatches',
    price: 21999,
    oldPrice: 26999,
    rating: 4.5,
    reviewCount: 534,
    badge: 'Outdoor',
    stock: 31,
    featured: true,
    bestSeller: false,
    tagline: 'Rugged trail watch with offline maps and 20-day battery.',
    description:
      'Built for people who leave tarmac behind. The Titan Active stores full offline topographic maps, survives military-standard drop and temperature testing, and runs for twenty days between charges so a long trek never needs a power bank.',
    highlights: [
      'Offline topographic maps with breadcrumb navigation',
      'MIL-STD-810H tested for shock and temperature',
      '20-day smartwatch battery, 60 hours GPS tracking',
      'Barometric altimeter and 3-axis compass',
      'Sapphire crystal glass and stainless steel bezel'
    ],
    specs: {
      'Display': '1.4-inch transflective MIP, always-on',
      'Case Material': 'Fibre-reinforced polymer + steel bezel',
      'Glass': 'Sapphire crystal',
      'Navigation': 'Multi-band GNSS, offline TopoActive maps',
      'Battery': '20 days smartwatch, 60 hours GPS mode',
      'Durability': 'MIL-STD-810H, 10 ATM water resistance',
      'Sensors': 'Barometric altimeter, compass, heart rate, SpO2',
      'Connectivity': 'Bluetooth, ANT+, Wi-Fi',
      'Straps': '22mm silicone with extension included',
      'Warranty': '2 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'titan-active-gps-1.svg', IMG_BASE + 'titan-active-gps-2.svg', IMG_BASE + 'titan-active-gps-3.svg'],
    reviews: [
      { name: 'Gurpreet Singh', rating: 5, date: '19 June 2026', title: 'Took it on a Himalayan trek', text: 'Offline maps worked flawlessly with no signal for five days. Battery still had 40% left.' },
      { name: 'Ayesha Siddiqui', rating: 4, date: '08 June 2026', title: 'Tough as advertised', text: 'Screen is not as vivid as AMOLED but it is readable in direct sunlight, which matters more.' }
    ]
  },
  {
    id: 19,
    name: 'AuraBand Fit',
    brand: 'Aura',
    category: 'smartwatches',
    price: 3499,
    oldPrice: 4999,
    rating: 4.1,
    reviewCount: 5104,
    badge: 'Budget',
    stock: 210,
    featured: false,
    bestSeller: false,
    tagline: 'Slim fitness band with 21-day battery and SpO2 tracking.',
    description:
      'The AuraBand Fit is the easiest way to start tracking activity. It weighs 24 grams, sits comfortably during sleep, and needs charging roughly once every three weeks — so you actually keep wearing it.',
    highlights: [
      '1.47" colour AMOLED touchscreen',
      '21-day battery on a single charge',
      'Continuous heart rate and SpO2 monitoring',
      '60 sports modes with auto workout detect',
      '5 ATM water resistant, safe for swimming'
    ],
    specs: {
      'Display': '1.47-inch AMOLED, 194 x 368',
      'Case Material': 'Polycarbonate',
      'Health Sensors': 'Heart rate, SpO2, sleep, stress',
      'Battery': 'Up to 21 days typical use',
      'Water Resistance': '5 ATM (50m)',
      'Connectivity': 'Bluetooth 5.2',
      'Compatibility': 'Android 8+ and iOS 14+',
      'Weight': '24 g with strap',
      'Straps': 'TPU quick-release',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'auraband-fit-1.svg', IMG_BASE + 'auraband-fit-2.svg', IMG_BASE + 'auraband-fit-3.svg'],
    reviews: [
      { name: 'Sneha Pillai', rating: 4, date: '11 June 2026', title: 'Great starter band', text: 'Comfortable enough to sleep in and I only charge it once a month or so.' },
      { name: 'Abhay Kumar', rating: 4, date: '22 May 2026', title: 'Good value', text: 'Step count and heart rate seem accurate. App is simple to use.' }
    ]
  },

  /* ----------------------------------------------- GAMING ACCESSORIES -- */
  {
    id: 20,
    name: 'Vortex RGB Mechanical Keyboard',
    brand: 'Vortex',
    category: 'gaming',
    price: 8999,
    oldPrice: 12999,
    rating: 4.7,
    reviewCount: 1342,
    badge: 'Best Seller',
    stock: 52,
    featured: true,
    bestSeller: true,
    tagline: 'Hot-swappable 75% board with gasket mount and per-key RGB.',
    description:
      'A gasket-mounted plate and three layers of internal foam give the Vortex a deep, muted typing sound that most gaming keyboards never achieve. Hot-swap sockets mean you can change switches without ever touching a soldering iron.',
    highlights: [
      'Hot-swappable sockets — no soldering needed',
      'Gasket mount with foam dampening for deep sound',
      'Per-key RGB with onboard effect memory',
      'Tri-mode: USB-C, Bluetooth 5.2 and 2.4GHz wireless',
      'PBT double-shot keycaps that never shine'
    ],
    specs: {
      'Layout': '75% (82 keys) with rotary knob',
      'Switches': 'Vortex Red linear, hot-swappable (3/5 pin)',
      'Keycaps': 'PBT double-shot, Cherry profile',
      'Mount': 'Gasket mount with 3-layer foam',
      'Connectivity': 'USB-C wired, Bluetooth 5.2, 2.4GHz dongle',
      'Battery': '4000mAh, up to 200 hours (RGB off)',
      'Polling Rate': '1000Hz wired, 1000Hz 2.4GHz',
      'Backlight': 'Per-key RGB, south-facing',
      'Compatibility': 'Windows, macOS, Linux',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'vortex-rgb-keyboard-1.svg', IMG_BASE + 'vortex-rgb-keyboard-2.svg', IMG_BASE + 'vortex-rgb-keyboard-3.svg'],
    reviews: [
      { name: 'Shaurya Nanda', rating: 5, date: '25 June 2026', title: 'Sounds incredible out of the box', text: 'No modding needed. The foam and gasket mount do all the work. Typing on it is addictive.' },
      { name: 'Tanya Grover', rating: 5, date: '15 June 2026', title: 'Hot-swap is a game changer', text: 'Swapped to tactile switches in ten minutes. Build quality feels far above the price.' }
    ]
  },
  {
    id: 21,
    name: 'Raptor Pro Wireless Mouse',
    brand: 'Raptor',
    category: 'gaming',
    price: 6499,
    oldPrice: 8999,
    rating: 4.8,
    reviewCount: 976,
    badge: 'Pro Gear',
    stock: 44,
    featured: true,
    bestSeller: true,
    tagline: '54g featherweight with a 30K sensor and 8K polling.',
    description:
      'Fifty-four grams, no honeycomb holes, and an 8000Hz polling rate. The Raptor Pro is the mouse esports players actually use, with optical switches rated to 100 million clicks and zero debounce delay.',
    highlights: [
      'Ultra-light 54g solid shell — no holes',
      '30,000 DPI optical sensor, 750 IPS tracking',
      '8000Hz polling rate with the included dongle',
      'Optical switches rated for 100M clicks',
      '90-hour battery with USB-C fast charge'
    ],
    specs: {
      'Sensor': 'Raptor Focus 30K optical',
      'DPI Range': '100 - 30,000 (50 DPI steps)',
      'Max Speed': '750 IPS, 70G acceleration',
      'Polling Rate': 'Up to 8000Hz wireless',
      'Switches': 'Optical, 100 million click rated',
      'Weight': '54 g',
      'Battery': 'Up to 90 hours, USB-C charging',
      'Buttons': '6 programmable',
      'Onboard Memory': '5 profiles',
      'Warranty': '2 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'raptor-pro-mouse-1.svg', IMG_BASE + 'raptor-pro-mouse-2.svg', IMG_BASE + 'raptor-pro-mouse-3.svg'],
    reviews: [
      { name: 'Aryan Deshmukh', rating: 5, date: '27 June 2026', title: 'Flicks feel effortless', text: 'Coming from an 85g mouse this feels like nothing. My aim improved within a week.' },
      { name: 'Ira Bhattacharya', rating: 5, date: '09 June 2026', title: 'Battery lasts forever', text: 'Two weeks of daily gaming per charge. No lag whatsoever at 8K polling.' }
    ]
  },
  {
    id: 22,
    name: 'Nebula Elite Controller',
    brand: 'Nebula',
    category: 'gaming',
    price: 11999,
    oldPrice: 14999,
    rating: 4.6,
    reviewCount: 621,
    badge: null,
    stock: 26,
    featured: false,
    bestSeller: false,
    tagline: 'Hall-effect sticks, back paddles and swappable components.',
    description:
      'Hall-effect thumbsticks use magnets instead of physical contacts, which means the Nebula Elite will never develop stick drift. Four remappable back paddles and adjustable trigger stops give competitive players a real edge.',
    highlights: [
      'Hall-effect sticks — drift-free for life',
      'Four remappable back paddles',
      'Adjustable trigger stops for faster shots',
      'Swappable stick tops and D-pad',
      'Works with PC, console and mobile'
    ],
    specs: {
      'Sticks': 'Hall-effect magnetic, anti-drift',
      'Triggers': 'Hall-effect with 3-stage stop switch',
      'Back Paddles': '4 remappable, magnetically attached',
      'Connectivity': 'USB-C wired, Bluetooth 5.2, 2.4GHz',
      'Battery': '1400mAh, up to 25 hours',
      'Compatibility': 'PC, Xbox, Android, iOS, Switch',
      'Haptics': 'Dual rumble motors + trigger vibration',
      'Weight': '295 g',
      'In the Box': 'Controller, case, USB-C cable, extra stick tops',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'nebula-elite-pad-1.svg', IMG_BASE + 'nebula-elite-pad-2.svg', IMG_BASE + 'nebula-elite-pad-3.svg'],
    reviews: [
      { name: 'Rudra Saxena', rating: 5, date: '20 June 2026', title: 'No more stick drift', text: 'Third controller in two years but the first with hall-effect sticks. Never going back.' }
    ]
  },
  {
    id: 23,
    name: 'StormCall 7.1 Gaming Headset',
    brand: 'Vortex',
    category: 'gaming',
    price: 5499,
    oldPrice: 7999,
    rating: 4.4,
    reviewCount: 1487,
    badge: null,
    stock: 71,
    featured: false,
    bestSeller: true,
    tagline: 'Virtual 7.1 surround with a broadcast-grade detachable mic.',
    description:
      'Positional audio that actually helps you locate footsteps, plus a detachable cardioid microphone that teammates consistently describe as clearer than most standalone mics. Memory-foam cups with cooling gel handle marathon sessions.',
    highlights: [
      'Virtual 7.1 surround sound for positional audio',
      'Detachable cardioid mic with pop filter',
      '50mm neodymium drivers',
      'Cooling-gel memory foam earcups',
      'Onboard volume dial and mic mute switch'
    ],
    specs: {
      'Driver': '50mm neodymium',
      'Surround': 'Virtual 7.1 (software enabled)',
      'Microphone': 'Detachable cardioid, noise cancelling',
      'Frequency Response': '20Hz - 20kHz',
      'Connectivity': 'USB-A and 3.5mm dual mode',
      'Cable Length': '2.2 m braided',
      'Weight': '312 g',
      'Compatibility': 'PC, PS5, Xbox, Switch, mobile',
      'Lighting': 'RGB earcup rings',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'stormcall-headset-1.svg', IMG_BASE + 'stormcall-headset-2.svg', IMG_BASE + 'stormcall-headset-3.svg'],
    reviews: [
      { name: 'Vivaan Chopra', rating: 4, date: '14 June 2026', title: 'Mic quality surprised me', text: 'Teammates asked what mic I upgraded to. It was just the one attached to this headset.' },
      { name: 'Anjali Raghavan', rating: 5, date: '01 June 2026', title: 'Comfortable for hours', text: 'The gel cups make a real difference. No ear fatigue after long sessions.' }
    ]
  },
  {
    id: 24,
    name: 'Apex Racing Gaming Chair',
    brand: 'Apex',
    category: 'gaming',
    price: 18999,
    oldPrice: 24999,
    rating: 4.3,
    reviewCount: 738,
    badge: 'Free Assembly',
    stock: 18,
    featured: true,
    bestSeller: false,
    tagline: 'Ergonomic recliner with 4D armrests and lumbar support.',
    description:
      'A cold-cure moulded foam seat holds its shape after years of use where cheap chairs flatten within months. Adjustable lumbar support, 4D armrests and a 155-degree recline let you dial in a position that works for both gaming and studying.',
    highlights: [
      'Cold-cure moulded foam that resists flattening',
      'Adjustable lumbar support and memory-foam headrest',
      '4D armrests move in every direction',
      'Reclines from 90 to 155 degrees with tilt lock',
      'Class-4 gas lift rated to 150kg'
    ],
    specs: {
      'Upholstery': 'Breathable PU leather with mesh panels',
      'Foam': 'Cold-cure moulded high-density foam',
      'Armrests': '4D adjustable (height, depth, width, pivot)',
      'Recline': '90 to 155 degrees with tilt lock',
      'Gas Lift': 'Class-4 certified, 150 kg capacity',
      'Base': 'Reinforced nylon with 60mm PU castors',
      'Recommended Height': '5\'4" to 6\'3"',
      'Assembly': 'Free assembly service included',
      'Weight': '23 kg',
      'Warranty': '3 Year Frame Warranty'
    },
    images: [IMG_BASE + 'apex-racing-chair-1.svg', IMG_BASE + 'apex-racing-chair-2.svg', IMG_BASE + 'apex-racing-chair-3.svg'],
    reviews: [
      { name: 'Kunal Bajaj', rating: 4, date: '18 June 2026', title: 'Back pain gone', text: 'Studying for eight hours a day used to wreck my back. The lumbar support genuinely helps.' },
      { name: 'Mahira Kaul', rating: 4, date: '30 May 2026', title: 'Assembly service was great', text: 'Two people came and built it in twenty minutes. Chair itself is very comfortable.' }
    ]
  },
  {
    id: 25,
    name: 'BoomBlast Party Speaker',
    brand: 'BassKing',
    category: 'gaming',
    price: 9999,
    oldPrice: 13999,
    rating: 4.5,
    reviewCount: 892,
    badge: null,
    stock: 37,
    featured: false,
    bestSeller: false,
    tagline: '80W stereo output with RGB lights and 24-hour battery.',
    description:
      'Eighty watts of stereo output with a dedicated passive radiator means the BoomBlast fills a room or a terrace without distorting. Reactive RGB lighting pulses to the beat, and two units can pair wirelessly for true stereo.',
    highlights: [
      '80W stereo output with dual passive radiators',
      'Beat-reactive RGB lighting ring',
      '24-hour playback at moderate volume',
      'TWS pairing — link two speakers for stereo',
      'IPX6 rated, safe for poolside and rain'
    ],
    specs: {
      'Output Power': '80W RMS stereo',
      'Drivers': '2x 40W full range + 2 passive radiators',
      'Battery': '10000mAh, up to 24 hours',
      'Charging': 'USB-C, doubles as a power bank',
      'Bluetooth': 'v5.3 with TWS pairing',
      'Inputs': 'Bluetooth, 3.5mm AUX, USB, microSD',
      'Lighting': 'Reactive RGB ring, 6 modes',
      'Water Resistance': 'IPX6',
      'Weight': '3.1 kg',
      'Warranty': '1 Year Manufacturer Warranty'
    },
    images: [IMG_BASE + 'boomblast-speaker-1.svg', IMG_BASE + 'boomblast-speaker-2.svg', IMG_BASE + 'boomblast-speaker-3.svg'],
    reviews: [
      { name: 'Jai Oberoi', rating: 5, date: '07 June 2026', title: 'Loud enough for a terrace party', text: 'Ran it at 70% volume for six hours and it still had battery left. Bass is punchy.' },
      { name: 'Lakshmi Nambiar', rating: 4, date: '24 May 2026', title: 'Great sound, heavy to carry', text: 'Sounds fantastic but at 3kg it is not something you toss in a backpack.' }
    ]
  }
];

/* Helper lookups used across pages ---------------------------------------- */

/** Returns a single product by its numeric id (or undefined). */
function getProductById(id) {
  return PRODUCTS.find(p => p.id === Number(id));
}

/** Returns the display name of a category id, e.g. "gaming" -> "Gaming Accessories". */
function getCategoryName(categoryId) {
  const cat = CATEGORIES.find(c => c.id === categoryId);
  return cat ? cat.name : categoryId;
}

/** Returns every product belonging to a category id. */
function getProductsByCategory(categoryId) {
  return PRODUCTS.filter(p => p.category === categoryId);
}

/** Products flagged as related: same category, excluding the current product. */
function getRelatedProducts(product, limit = 4) {
  return PRODUCTS
    .filter(p => p.category === product.category && p.id !== product.id)
    .slice(0, limit);
}
