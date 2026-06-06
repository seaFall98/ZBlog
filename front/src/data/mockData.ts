// Mock data for the blog

export interface Post {
  id: string;
  title: string;
  excerpt: string;
  content: string;
  category: string;
  tags: string[];
  date: string;
  readTime: number;
  coverImage: string;
  featured?: boolean;
}

export interface Album {
  id: string;
  title: string;
  description: string;
  coverImage: string;
  photoCount: number;
  date: string;
}

export interface Photo {
  id: string;
  albumId: string;
  src: string;
  title: string;
  description: string;
  date: string;
  filename: string;
}

export interface Moment {
  id: string;
  text: string;
  images: string[];
  date: string;
  mood: string;
}

export interface GuestMessage {
  id: string;
  name: string;
  content: string;
  date: string;
  avatar: string;
}

export interface FriendLink {
  id: string;
  name: string;
  url: string;
  description: string;
  logo: string;
  category: string;
}

export const categories = [
  { id: "writing", name: "写作", description: "关于写作的思考与实践", count: 18, coverImage: "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=600&q=80" },
  { id: "reading", name: "阅读", description: "书籍、评论与思想碎片", count: 14, coverImage: "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=600&q=80" },
  { id: "travel", name: "旅行", description: "路途中的风景与感悟", count: 11, coverImage: "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=600&q=80" },
  { id: "life", name: "生活", description: "日常的细小惊喜", count: 23, coverImage: "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&q=80" },
  { id: "film", name: "影像", description: "镜头与光影的叙事", count: 9, coverImage: "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600&q=80" },
  { id: "thoughts", name: "随想", description: "未成形的念头与灵感", count: 16, coverImage: "https://images.unsplash.com/photo-1518655048521-f130df041f66?w=600&q=80" },
];

export const tags = [
  { name: "秋日", count: 24 }, { name: "读书笔记", count: 19 },
  { name: "散文", count: 17 }, { name: "摄影", count: 22 },
  { name: "城市", count: 15 }, { name: "咖啡", count: 11 },
  { name: "日记", count: 28 }, { name: "电影", count: 13 },
  { name: "音乐", count: 9 }, { name: "建筑", count: 8 },
  { name: "植物", count: 14 }, { name: "光", count: 20 },
  { name: "旅途", count: 16 }, { name: "夜晚", count: 12 },
  { name: "书信", count: 7 }, { name: "记忆", count: 18 },
  { name: "孤独", count: 6 }, { name: "季节", count: 21 },
  { name: "语言", count: 10 }, { name: "时间", count: 25 },
  { name: "梦境", count: 5 }, { name: "色彩", count: 13 },
];

export const posts: Post[] = [
  {
    id: "autumn-light",
    title: "秋日午后的光线总是来得比预想中更温柔",
    excerpt: "窗外的梧桐叶还挂着，金黄中带一点锈色，风一吹，就有几片打着旋儿落下来。这样的下午，适合什么都不做。",
    content: `<h2>光的温度</h2><p>窗外的梧桐叶还挂着，金黄中带一点锈色，风一吹，就有几片打着旋儿落下来。这样的下午，适合什么都不做，只是坐在那里，看光从窗缝里斜进来，落在书桌的一角，慢慢移动。</p><p>我向来喜欢这种不确定的时刻。早上的光是明确的，有方向，有任务感；但下午三四点钟的光不同，它似乎没有目的，只是随意流淌，打在哪里算哪里。坐在光里的人，也变得有些透明。</p><blockquote>每一个平凡的下午都藏着某种神圣的东西，只是我们太忙，忘记了停下来接住它。</blockquote><h2>关于停下来</h2><p>有时候我想，所谓生活的质感，不是由那些大事构成的——不是升职、旅行、或者某次奔赴——而是由这些小小的静止时刻累积而成。一杯茶的温度。一本书读到某句话时忽然停住。窗外某棵树的轮廓。</p><p>这些东西无法拍照，无法转述，只能独自持有。就像秋日的光线，你知道它很好，但说不出哪里好。</p>`,
    category: "生活",
    tags: ["秋日", "日记", "光", "季节"],
    date: "2024-10-24",
    readTime: 5,
    coverImage: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=1200&q=80",
    featured: true,
  },
  {
    id: "on-reading-slowly",
    title: "慢读：重新学习用眼睛触碰文字",
    excerpt: "我们已经太习惯快速阅读了——扫描，提取，关闭。但真正的阅读需要摩擦，需要停顿，需要一点阻力。",
    content: `<h2>速度的代价</h2><p>我们已经太习惯快速阅读了——扫描，提取，关闭。手机让我们的眼睛习惯了滑动，而不是停留。但真正的阅读需要摩擦，需要停顿，需要一点阻力。</p><p>当我们慢下来，文字才开始有重量。一个句子，反复读两三遍，它的意思就像一块石头在水里，慢慢沉到更深的地方。</p><h2>身体阅读</h2><p>慢读不只是速度的问题，它是一种身体姿态。用手指触碰书页，听翻页的声音，闻到纸的气味——这些都是阅读的一部分。电子屏幕抹去了所有这些触感，阅读变成了纯粹的信息摄入。</p><blockquote>好的阅读者，是用整个身体在读，而不仅仅是用眼睛。</blockquote>`,
    category: "阅读",
    tags: ["读书笔记", "散文", "语言"],
    date: "2024-10-18",
    readTime: 7,
    coverImage: "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=1200&q=80",
    featured: true,
  },
  {
    id: "city-at-dusk",
    title: "城市在黄昏时会短暂地变得可爱",
    excerpt: "太阳快落山的那二十分钟，城市里所有坚硬的东西都软化了。玻璃幕墙变成橙色的镜子，路面反光，人们的脸被镀了一层暖调。",
    content: `<h2>黄金二十分钟</h2><p>太阳快落山的那二十分钟，城市里所有坚硬的东西都软化了。玻璃幕墙变成橙色的镜子，路面反光，人们的脸被镀了一层暖调。这个城市，平时充满噪音和效率，在这一刻忽然变得有点动人。</p><p>我总是在这时候出门。不为什么，就是想在城市最可爱的时候走在它里面。</p>`,
    category: "旅行",
    tags: ["城市", "光", "夜晚"],
    date: "2024-10-10",
    readTime: 4,
    coverImage: "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=1200&q=80",
    featured: true,
  },
  {
    id: "on-letters",
    title: "写信这件事，在消失之前值得被记录",
    excerpt: "我收到最后一封手写信，是2017年。寄信人是我的外婆，信纸是淡蓝色的，字迹有些颤抖。",
    content: `<h2>最后的手写信</h2><p>我收到最后一封手写信，是2017年。寄信人是我的外婆，信纸是淡蓝色的，字迹有些颤抖。她在信里说，今天下了雨，买了一把新伞，想起我小时候特别怕雷声。</p><p>就这样几件小事。但我读了很多遍。</p><h2>信的重量</h2><p>写信是一种非常缓慢的关心。你要找笔，找纸，想好要说什么，再一笔一画写下来，装进信封，走到邮局。整个过程，这份关心已经被反复确认了很多次。</p><blockquote>每一封信都是一次愿意慢下来的证明。</blockquote>`,
    category: "写作",
    tags: ["书信", "记忆", "时间"],
    date: "2024-09-28",
    readTime: 8,
    coverImage: "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=1200&q=80",
  },
  {
    id: "coffee-ritual",
    title: "咖啡仪式：一个人的早晨需要一点庄重感",
    excerpt: "每天早上，我会花十五分钟手冲咖啡。不只是因为它好喝，更因为这个过程本身。",
    content: `<h2>仪式的意义</h2><p>每天早上，我会花十五分钟手冲咖啡。不只是因为它好喝，更因为这个过程本身——磨豆，烧水，等待，注水，再等待。这是一天中最安静的十五分钟。</p><p>仪式感不是矫情，它是一种自我尊重的方式。当你愿意为自己花这点时间，你在告诉自己：这一天值得被认真对待。</p>`,
    category: "生活",
    tags: ["咖啡", "日记", "季节"],
    date: "2024-09-15",
    readTime: 4,
    coverImage: "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80",
  },
  {
    id: "film-photography",
    title: "胶片摄影教会我的几件事",
    excerpt: "用胶片拍照，每一次快门都是一次承诺。你不能无限重拍，不能即时查看，你必须信任自己的判断。",
    content: `<h2>稀缺感的价值</h2><p>用胶片拍照，每一次快门都是一次承诺。你不能无限重拍，不能即时查看，你必须信任自己的判断——这道光够了吗？这个构图对吗？</p><p>数码相机抹去了这种不确定性，但也带走了那份期待感。两周后从冲洗店取回照片，看到那张你已经半忘记的光线，这才是摄影最好的部分。</p>`,
    category: "影像",
    tags: ["摄影", "电影", "色彩"],
    date: "2024-09-02",
    readTime: 6,
    coverImage: "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=1200&q=80",
  },
  {
    id: "on-solitude",
    title: "孤独有时候是一种奢侈",
    excerpt: "不是所有人都能承受真正的孤独，而我花了很多年才学会把它当作礼物而不是惩罚。",
    content: `<h2>孤独的质地</h2><p>不是所有人都能承受真正的孤独，而我花了很多年才学会把它当作礼物而不是惩罚。当你独自在一个地方，没有手机，没有音乐，没有任何填充物，你才能真正听到自己在想什么。</p><blockquote>孤独是一个人和自己相处的能力。这个能力越强，你越自由。</blockquote>`,
    category: "随想",
    tags: ["孤独", "时间", "梦境"],
    date: "2024-08-20",
    readTime: 5,
    coverImage: "https://images.unsplash.com/photo-1518655048521-f130df041f66?w=1200&q=80",
  },
  {
    id: "architecture-walk",
    title: "城市行走：用脚步丈量一座建筑的温度",
    excerpt: "那栋老楼的清水混凝土已经有了岁月的痕迹，光打在上面有一种温柔的粗粝感。",
    content: `<h2>混凝土的温度</h2><p>那栋老楼的清水混凝土已经有了岁月的痕迹，光打在上面有一种温柔的粗粝感。我在它面前站了很久，试图理解为什么这个东西会让我这么平静。</p><p>好的建筑不说话，但它让你觉得你可以停下来。</p>`,
    category: "旅行",
    tags: ["建筑", "城市", "旅途"],
    date: "2024-08-05",
    readTime: 6,
    coverImage: "https://images.unsplash.com/photo-1486325212027-8081e485255e?w=1200&q=80",
  },
];

export const albums: Album[] = [
  { id: "kyoto-2024", title: "京都·秋", description: "2024年秋天，十天的私人巡礼", coverImage: "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800&q=80", photoCount: 16, date: "2024-11" },
  { id: "shanghai-streets", title: "上海街角", description: "城市的毛细血管里，那些被忽略的细节", coverImage: "https://images.unsplash.com/photo-1474181487882-5abf3f0ba6c2?w=800&q=80", photoCount: 12, date: "2024-09" },
  { id: "winter-light", title: "冬日光影", description: "冬天的光线总是带着某种哀愁", coverImage: "https://images.unsplash.com/photo-1517685352821-92cf88aee5a5?w=800&q=80", photoCount: 14, date: "2024-01" },
  { id: "botanicals", title: "植物研究", description: "细小的叶片，无声的生长", coverImage: "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=800&q=80", photoCount: 10, date: "2023-08" },
];

export const galleryPhotos: Photo[] = [
  { id: "p1", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=900&q=85", title: "金阁寺晨雾", description: "清晨六点，雾气还没散开，金阁寺倒映在水里，有一种梦的质感。", date: "2024-11-03", filename: "kyoto_001.jpg" },
  { id: "p2", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=900&q=85", title: "岚山竹林", description: "竹子很高，光从缝隙里碎下来，踩着影子走，安静得像一首无词歌。", date: "2024-11-04", filename: "kyoto_002.jpg" },
  { id: "p3", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=900&q=85", title: "祗园石板路", description: "下雨天的祗园最好看，石板反光，屋檐滴水，时间好像停了。", date: "2024-11-05", filename: "kyoto_003.jpg" },
  { id: "p4", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=900&q=85", title: "枯山水", description: "白砂被耙出波纹，一块石头，什么都没有，又好像什么都有了。", date: "2024-11-06", filename: "kyoto_004.jpg" },
  { id: "p5", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1578469645742-46cae010e5d4?w=900&q=85", title: "红叶坠落", description: "枫叶落在石阶上，像是谁把画撕碎了，随意散着。", date: "2024-11-07", filename: "kyoto_005.jpg" },
  { id: "p6", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=900&q=85", title: "寺庙门口", description: "一个老僧人走过，袈裟的橙色和银杏叶的黄，撞出了一种诗意。", date: "2024-11-08", filename: "kyoto_006.jpg" },
  { id: "p7", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=900&q=85", title: "鸟居群影", description: "数不清的鸟居排成隧道，红色在黄昏里像要燃起来。", date: "2024-11-09", filename: "kyoto_007.jpg" },
  { id: "p8", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1486299267070-83823f5448dd?w=900&q=85", title: "市场的早晨", description: "锦市场的商贩刚开店，豆腐、咸鱼、腌菜——生活气息扑面而来。", date: "2024-11-10", filename: "kyoto_008.jpg" },
  { id: "p9", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1567016376408-0226e4d0c1ea?w=900&q=85", title: "茶室窗景", description: "一格木窗，窗外是一株老梅，这就是所谓的借景。", date: "2024-11-11", filename: "kyoto_009.jpg" },
  { id: "p10", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1460627390041-532a28402358?w=900&q=85", title: "夜之寂静", description: "夜晚的京都不属于游客，它把最好的一面留给了独自走夜路的人。", date: "2024-11-12", filename: "kyoto_010.jpg" },
  { id: "p11", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1512236393565-3a06b61b8e42?w=900&q=85", title: "石庭倒影", description: "水面上天光云影，和石头的沉默对话。", date: "2024-11-13", filename: "kyoto_011.jpg" },
  { id: "p12", albumId: "kyoto-2024", src: "https://images.unsplash.com/photo-1583416750470-965b2707b355?w=900&q=85", title: "秋色渐深", description: "最后一天，树上的叶子已经红透，再待两天就要全落了。", date: "2024-11-13", filename: "kyoto_012.jpg" },
];

export const moments: Moment[] = [
  { id: "m1", text: "今天的咖啡泡得很好，奶泡细腻，在光线里像一片云。窗外下着小雨，不想出门，不想做事，只是坐着，很满足。", images: ["https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600&q=80"], date: "2024-10-25", mood: "慵懒" },
  { id: "m2", text: "在旧书市场发现一本1973年的《植物图鉴》，手绘插图，纸张已经泛黄，但每一页都是认真的。买了，放在书架上没打算读，只是喜欢它在那里。", images: ["https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=600&q=80", "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=600&q=80"], date: "2024-10-20", mood: "满足" },
  { id: "m3", text: "今晚散步，路过一家卖花的小店，老板在给白色的花束系丝带，动作很慢，很仔细。我站在外面看了很久，没进去，也没走开。", images: [], date: "2024-10-15", mood: "平静" },
  { id: "m4", text: "下午三点，窗外的梧桐树上有只鸟，叫了几声，停了一会儿，又叫。时间就这样过去了二十分钟。我也不知道我在等什么。", images: ["https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&q=80"], date: "2024-10-08", mood: "空旷" },
  { id: "m5", text: "收拾房间，从抽屉底部翻出一张六年前的电影票。那天的天气，现在完全记不得了。但我记得散场时外面正在下雨，我们没带伞。", images: [], date: "2024-09-30", mood: "怀旧" },
  { id: "m6", text: "在公园里拍了一组枯叶的照片，用的旧相机，不知道效果怎么样。但拍的时候很专注，专注到忘记了其他一切事情。这种状态很少有。", images: ["https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600&q=80", "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=600&q=80"], date: "2024-09-22", mood: "专注" },
];

export const guestMessages: GuestMessage[] = [
  { id: "gm1", name: "云上的月亮", content: "每次看你的文章都有一种被轻轻按住的感觉，谢谢你写这些东西。", date: "2024-10-23", avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=80&q=80" },
  { id: "gm2", name: "一只猫", content: "秋日那篇，读了三遍。最后那句\"只是独自持有\"，记住了。", date: "2024-10-21", avatar: "https://images.unsplash.com/photo-1527980965255-d3b416303d12?w=80&q=80" },
  { id: "gm3", name: "南风知我意", content: "我也是那种会在书上折角的人，看到你说\"慢读\"感觉找到了同类。", date: "2024-10-18", avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=80&q=80" },
  { id: "gm4", name: "路过的旅人", content: "相册里京都的照片让我想起上次去日本，那种安静感是真实的。", date: "2024-10-15", avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=80&q=80" },
  { id: "gm5", name: "松柏青青", content: "博客做得真好，看完好久都不想刷手机了。这是很高的评价。", date: "2024-10-10", avatar: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=80&q=80" },
  { id: "gm6", name: "海棠开了", content: "胶片那篇读了，去找出压箱底的相机。谢谢提醒我还有这个东西。", date: "2024-10-05", avatar: "https://images.unsplash.com/photo-1544725176-7c40e5a71c5e?w=80&q=80" },
];

export const friendLinks: FriendLink[] = [
  { id: "fl1", name: "字里行间", url: "#", description: "关于书写与阅读的安静角落", logo: "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=80&q=80", category: "写作" },
  { id: "fl2", name: "光影笔记", url: "#", description: "纪录片导演的影像思考", logo: "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=80&q=80", category: "影像" },
  { id: "fl3", name: "旅途拾遗", url: "#", description: "独自旅行的人的自言自语", logo: "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=80&q=80", category: "旅行" },
  { id: "fl4", name: "素日记", url: "#", description: "记录平凡生活里的小美好", logo: "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=80&q=80", category: "生活" },
  { id: "fl5", name: "设计余白", url: "#", description: "关于设计和审美的个人观察", logo: "https://images.unsplash.com/photo-1486325212027-8081e485255e?w=80&q=80", category: "设计" },
  { id: "fl6", name: "植物人类学", url: "#", description: "用植物理解人类与自然的关系", logo: "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=80&q=80", category: "自然" },
];

export const siteStats = {
  totalPosts: 91,
  totalVisits: 48620,
  totalPhotos: 284,
  totalMessages: 347,
  monthlyVisits: [
    { month: "5月", visits: 3200, posts: 6 },
    { month: "6月", visits: 3800, posts: 8 },
    { month: "7月", visits: 4100, posts: 7 },
    { month: "8月", visits: 4600, posts: 9 },
    { month: "9月", visits: 5200, posts: 8 },
    { month: "10月", visits: 6100, posts: 10 },
  ],
  topPosts: [
    { title: "秋日午后的光线总是来得比预想中更温柔", views: 4821, id: "autumn-light" },
    { title: "慢读：重新学习用眼睛触碰文字", views: 3640, id: "on-reading-slowly" },
    { title: "写信这件事，在消失之前值得被记录", views: 3012, id: "on-letters" },
    { title: "城市在黄昏时会短暂地变得可爱", views: 2856, id: "city-at-dusk" },
    { title: "胶片摄影教会我的几件事", views: 2488, id: "film-photography" },
  ],
};
