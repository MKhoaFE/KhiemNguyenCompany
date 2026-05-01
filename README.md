📦 Card Dealer Management (Desktop) – v2.0

Desktop application (Java Swing) for managing prepaid cards and coffee sales at a retail counter. Runs fully offline, uses SQLite for storage, and can be packaged into a simple .exe for daily use.

📍 Location: 66 Ly Nam De
🗓 Updated: May 1, 2026
🏢 Organization: KhiemNguyenCompany

⚙️ 1. REQUIREMENTS

To run this project, you need the following tools installed:

Java Development Kit (JDK 17 or higher)
IntelliJ IDEA (recommended IDE)
SQLite (lightweight database engine)
DB Browser for SQLite (for viewing/editing database)
Apache Maven (for dependency management & build)

Optional (for packaging):

Launch4j (convert .jar → .exe)
🚀 2. OVERVIEW

This application helps manage daily operations at a small retail shop:

Opening inventory
Goods receiving
Sales orders
Invoice printing (A5)
Closing inventory
Revenue reports
Price list management

👉 No internet required
👉 No server required
👉 Data stored locally in a .db file

🧱 3. ARCHITECTURE

Simple layered architecture:

UI (Swing)
  ↓
Service (Business Logic)
  ↓
DAO (JDBC)
  ↓
SQLite Database (.db file)
🖥 4. USER INTERFACE
Built with Java Swing
Main window (JFrame)
Navigation via JTabbedPane

Tabs:

F1 – Opening Inventory
F2 – Goods Received
F3 – Sales Orders
F4 – Closing Inventory
F5 – Reports
F6 – Price List
⌨️ 5. SHORTCUT KEYS
Key	Function
F1	Opening Inventory
F2	Goods Received
F3	Sales
F4	Closing Inventory
F5	Reports
F6	Price List
🗄 6. DATABASE
Type: SQLite
File: database/data.db
Runs locally with the application

Main tables:

card_types
inventory_entries
orders
order_items

👉 Database can be opened and managed using DB Browser for SQLite

📂 7. PROJECT STRUCTURE
project/
├── src/
│   ├── ui/
│   ├── dao/
│   ├── model/
│   ├── service/
│   └── db/
├── database/
│   └── data.db
└── pom.xml
📦 8. BUILD & RUN
Build using Maven
Output: .jar file
Can be wrapped into .exe using Launch4j
💾 9. DATA STORAGE
Stored locally in data.db
No cloud, no remote server
Backup = copy the .db file
✅ 10. ADVANTAGES
Fully offline
Lightweight & fast
Easy deployment (just .exe)
No infrastructure required
Suitable for small retail shops
🔮 11. FUTURE IMPROVEMENTS
Auto database backup
LAN multi-user support
Excel export
Dark mode
Thermal printer support
📌 NOTES
Application requires SQLite database file to exist
Dependencies managed via Maven
Designed for Windows desktop usage
