📦 CARD DEALER MANAGEMENT (DESKTOP) – v2.0

Organization: KhiemNguyenCompany
Location: 66 Ly Nam De
Updated: May 1, 2026

⚙️ 1. REQUIREMENTS

To run this application, make sure the following tools are available:

✔️ Java Development Kit (JDK 17 or higher)
✔️ IntelliJ IDEA (recommended IDE)
✔️ SQLite (local database)
✔️ DB Browser for SQLite (database viewer/editor)
✔️ Apache Maven (build & dependency management)

Optional:

➕ Launch4j (to package .jar → .exe)
🚀 2. APPLICATION OVERVIEW

This is a desktop application (Java Swing) designed for small retail operations.

🔹 Main capabilities:
• Manage opening inventory
• Record incoming goods
• Create sales orders
• Print invoices (A5 format)
• Track closing inventory
• Generate revenue reports
• Maintain price list
🔹 Key characteristics:
✅ 100% Offline
✅ No server required
✅ No internet required
✅ Data stored locally (.db file)
🧱 3. SYSTEM ARCHITECTURE

The project follows a simple layered structure:

UI (Swing)
   ↓
Service (Business Logic)
   ↓
DAO (JDBC)
   ↓
SQLite Database
🖥 4. USER INTERFACE
🔹 Structure:
Main window: JFrame
Navigation: JTabbedPane
🔹 Tabs:
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
🔹 Type:
SQLite (embedded database)
🔹 File location:
database/data.db
🔹 Main tables:
card_types
inventory_entries
orders
order_items
🔹 Notes:
✔️ Runs locally with the app
✔️ Can be opened using DB Browser for SQLite
✔️ No database server needed
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
📦 8. BUILD & DEPLOY
🔹 Build:
Use Maven to package the project
Output: .jar file
🔹 Deployment:
Optionally convert .jar → .exe using Launch4j
🔹 Example release structure:
release/
├── app.exe
├── app.jar
├── database/
│   └── data.db
└── jre/ (optional)
💾 9. DATA STORAGE
📁 Stored locally in data.db
🔄 Backup: copy the .db file
🌐 No cloud / remote connection
✅ 10. ADVANTAGES
✔️ Fully offline operation
✔️ Lightweight and fast
✔️ Easy deployment (single .exe)
✔️ No infrastructure required
✔️ Suitable for small retail environments
🔮 11. FUTURE IMPROVEMENTS
• Automatic database backup
• Multi-user (LAN support)
• Excel export
• Dark mode UI
• Thermal printer integration
📌 12. NOTES
• Requires existing SQLite database file
• Built with Java 17+
• Dependencies managed via Maven
• Designed for Windows desktop usage
