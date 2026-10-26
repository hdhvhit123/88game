# HGames: 88 Offline Games

88 trò chơi native cho Android, chơi offline hoàn toàn.

Ứng dụng Android viết hoàn toàn bằng **Kotlin**, mỗi trò chơi tự vẽ trên `Canvas` và chạy bằng vòng lặp khung hình `Choreographer`.
Ứng dụng **không dùng WebView** và không cần mạng, cũng không có quảng cáo hay quyền truy cập đặc biệt (chỉ dùng quyền rung).

Danh sách trò chơi được dựng lại dựa trên bộ sưu tập ở wugames.io, nhưng **toàn bộ mã nguồn, hình ảnh và âm thanh đều là bản gốc**:
hình vẽ bằng vector/Canvas và emoji hệ thống, âm thanh được tổng hợp lúc chạy. Ứng dụng không sao chép tài nguyên, mã hay thương hiệu nào của trang web đó.

## Tính năng

- 88 trò chơi chia 8 nhóm, có tìm kiếm không dấu, mục yêu thích và mục chơi gần đây
- Mỗi trò có hướng dẫn, nút chơi lại, tuỳ chọn độ khó hoặc chế độ (nếu có) và lưu kỷ lục
- Các AI chơi cờ chạy trên luồng nền: cờ vua và cờ tướng dùng alpha-beta, Ultimate Tic-Tac-Toe dùng Monte Carlo
- Các màn chơi được sinh ngẫu nhiên và luôn giải được (Sudoku, Water Sort, Flow, Block Escape, Mahjong…)
- Có hiệu ứng hạt, chuyển động mượt, rung phản hồi và âm thanh, đều có thể tắt
- minSdk 24 (Android 7.0), targetSdk 36

## Danh sách trò chơi

**Arcade** (13)

- 🐍 **Snake** – Rắn săn mồi
- 🧱 **Block Drop** – Xếp gạch rơi
- 🏓 **Breakout** – Phá gạch bằng bóng
- 🐦 **Flappy Bird** – Vỗ cánh luồn ống
- 🦖 **Dino Run** – Khủng long vượt chướng ngại
- 👻 **Pac Maze** – Ăn chấm tránh ma
- 🥞 **Stack** – Xếp tháp khối
- 🌉 **Stick Hero** – Kéo gậy làm cầu
- 🍥 **Helix Jump** – Bóng rơi qua tháp xoắn
- 🔄 **Color Switch** – Bóng đổi màu vượt vòng
- 🔪 **Knife Hit** – Phóng dao vào khúc gỗ
- 🦘 **Sky Jump** – Nhảy bậc lên trời
- 🐔 **Cross Road** – Gà băng qua đường

**Trí tuệ** (21)

- 🧪 **Water Sort Puzzle** – Rót nước phân màu
- 📦 **Sokoban** – Đẩy thùng vào đích
- 🔢 **2048** – Gộp số đến 2048
- ✏️ **Sudoku** – Điền số 1–9 logic
- 💣 **Minesweeper** – Dò mìn kinh điển
- 🔵 **Peg Solitaire** – Nhảy quân còn 1
- 💡 **Lights Out** – Tắt hết đèn
- 🧩 **Sliding Puzzle** – Trượt ô số 15
- 🗼 **Tower of Hanoi** – Chuyển tháp đĩa
- 🌊 **Flood It** – Phủ kín một màu
- ⚖️ **Futoshiki** – Điền số theo dấu lớn bé
- 🌀 **Maze** – Tìm đường ra mê cung
- 🚰 **Pipes** – Xoay ống nối nước
- 0️⃣ **Binary Puzzle** – Điền 0 và 1 cân bằng
- 🔗 **Tile Connect** – Nối cặp hình giống nhau
- 🚘 **Block Escape** – Giải phóng khối đỏ
- 〰️ **Flow Connect** – Nối cặp màu kín bàn
- 🖼️ **Nonogram** – Tô ô theo gợi ý số
- 🎋 **Mahjong Solitaire** – Ghép cặp mạt chược
- 🔐 **Mastermind** – Đoán mã màu bí mật
- 🟦 **Block Puzzle** – Xếp khối xoá hàng

**Cờ & bàn** (12)

- ❌ **Tic-Tac-Toe** – Cờ ca-rô 3×3
- ♟️ **Chess** – Cờ vua đấu máy
- 🀄 **Xiangqi** – Cờ tướng đấu máy
- 👑 **Checkers** – Cờ đam
- ⚫ **Gomoku** – Cờ ca-rô 5 con
- ⚪ **Reversi** – Othello lật quân
- 🔴 **Connect Four** – Thả đĩa 4 liên tiếp
- #️⃣ **Ultimate Tic-Tac-Toe** – Ca-rô lồng nhau
- 🔲 **Dots and Boxes** – Nối chấm chiếm ô
- 🥜 **Mancala** – Ô ăn quan Kalah
- 🚢 **Battleship** – Hải chiến bắn tàu
- 🔳 **Dominoes** – Domino đôi 6

**Bài & casino** (8)

- 🃏 **Solitaire** – Xếp bài Klondike
- 🕷️ **Spider Solitaire** – Xếp bài nhện
- ♣️ **FreeCell** – Xếp bài ô trống
- 🔺 **Pyramid Solitaire** – Ghép cặp tổng 13
- ♠️ **Blackjack** – Xì dách 21 điểm
- 🎟️ **Bingo** – Dò số Bingo
- 🎲 **Yahtzee** – Tung xúc xắc tính điểm
- 🔻 **Plinko** – Thả bóng qua chốt

**Hành động** (10)

- 🚀 **Space Shooter** – Bắn phi thuyền ngoài không gian
- ☄️ **Asteroid Storm** – Bắn vỡ thiên thạch
- 🛰️ **Missile Defense** – Bảo vệ thành phố
- 🏍️ **Light Cycle** – Đua mô tô ánh sáng
- 📄 **Paper Territory** – Chiếm lãnh thổ
- 🕳️ **Hole.io** – Hố đen nuốt thành phố
- 🍉 **Fruit Slice** – Chém trái cây
- 🕹️ **Pinball** – Bắn bi điện tử
- 🏹 **Stickman Archer** – Đấu cung người que
- 🏰 **Tower Defense** – Xây tháp thủ thành

**Thể thao** (8)

- ⚽ **Penalty Kick** – Sút luân lưu 11m
- 🎯 **Darts** – Phi tiêu 501
- 🎳 **Bowling** – Ném bóng đổ pin
- ⛳ **Mini Golf** – Golf mini 9 lỗ
- 🏒 **Air Hockey** – Khúc côn cầu bàn hơi
- 🎱 **8 Ball Pool** – Bi-a 8 bi đấu máy
- 🏀 **Basketball** – Ném bóng vào rổ
- 🎾 **Tennis** – Quần vợt đấu máy

**Đua xe** (5)

- 🚙 **Hill Climb** – Leo đồi bằng xe
- 🏁 **Moto Trials** – Mô tô địa hình
- 🛵 **Moto Rush** – Mô tô luồn lách
- 🚗 **Car Racing** – Đua xe cao tốc
- 🏎️ **Circuit Racing** – Đua xe đường đua

**Giải trí** (11)

- 🎴 **Memory Match** – Lật thẻ tìm cặp
- 🎨 **Color Memory** – Nhớ chuỗi màu
- 🔨 **Whack-a-Mole** – Đập chuột chũi
- 💎 **Gem Match** – Ghép 3 viên đá quý
- 🔤 **Word Guess** – Đoán từ 5 chữ cái
- ❓ **Hangman** – Đoán chữ cứu người
- 🔍 **Word Search** – Tìm từ trong lưới
- 🎹 **Piano Tiles** – Chạm phím đàn đen
- ⌨️ **Typing Race** – Đua xe bằng gõ phím
- 🍒 **Fruit Merge** – Hợp nhất trái cây
- 🔮 **Bubble Shooter** – Bắn bóng cùng màu

## Build

Dùng Android Studio hoặc dòng lệnh:

```bash
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # unit test cho phần luật chơi (com.hgame.core)
```

Thư mục `tools/` có script build hoàn toàn offline (aapt2 + kotlinc + d8), dùng khi không tải được Android Gradle Plugin:

```bash
tools/build.sh      # -> build-offline/hgame.apk
tools/test.sh       # chạy unit test bằng JUnit
tools/selftest.sh   # cài lên thiết bị/emulator, tự chạm thử từng game, chụp màn hình và báo crash
```

GitHub Actions (`.github/workflows/android.yml`) chạy unit test và build APK debug/release cho mỗi lần push hoặc pull request.

## Cấu trúc mã

| Gói | Nội dung |
| --- | --- |
| `com.hgame.core` | Luật chơi thuần Kotlin, không phụ thuộc Android, có unit test (cờ vua, cờ tướng, Sudoku, 2048, sinh màn…) |
| `com.hgame.engine` | `Game` (lớp cơ sở), `GameView` (vòng lặp khung hình, cảm ứng đa điểm), đồ hoạ, bảng màu, âm thanh tổng hợp, widget |
| `com.hgame.games.*` | Giao diện và điều khiển của từng trò chơi, `Catalog` đăng ký toàn bộ 88 game |
| `com.hgame.app`, `com.hgame.ui` | Màn hình chính, màn hình chơi, thanh trên cùng, hộp kết quả, lưu cài đặt |

Để thêm một trò chơi: kế thừa `Game`, cài `newGame()` và `draw()` cùng các hàm xử lý chạm, rồi thêm một dòng vào `Catalog.all`.
