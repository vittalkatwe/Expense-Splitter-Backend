# Expense Split Backend

This project provides the backend services for an Expense Splitter application. It allows users to create groups, add expenses, split them fairly among group members and settle debts. The application is built using Java and Spring Boot, and it utilizes an in-memory H2 database for easy testing and development.


### Key Features:

*   **Group Management:** Create groups and add users to share expenses.
*   **Expense Tracking:** Add bills for various purposes like dinners, trips, or rent.
*   **Flexible Splitting Options:**
    *   **Equal:** Every member pays an equal share.
    *   **Exact:** Specify the exact amount each person owes.
    *   **Percentage:** Divide the bill based on percentages.
*   **Multi-Currency Support:** Handle expenses in different currencies such as USD, INR, EUR, GBP and AUD.
*   **Balance Management:** Keep a clear record of how much each user has to pay and receive.
*   **Payment Recording:** Log payments to settle outstanding debts (Settlement Table).

## How to Get it Running

### Prerequisites:

*   **Java:** Version 17 or newer
*   **Maven:** A build automation tool

### Steps:

1.  **Get the code:**
    ```sh
    git clone https://github.com/vittalkatwe/Expense-Splitter-Backend.git
    cd expensesplitbackend
    ```

2.  **Set up the database:**
    Open the `src/main/resources/application.properties` file. For a straightforward testing experience, you can use the H2 in-memory database with the existing configuration or add below configuration for different database (Postgres for reference):
    ```properties
      spring.datasource.url=jdbc:postgresql://localhost:5432/dbname
      spring.datasource.username=postgres
      spring.datasource.password=your_password
      spring.datasource.driver-class-name=org.postgresql.Driver
      spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
      spring.jpa.hibernate.ddl-auto=update   # or create-drop if you want schema to reset every time
      spring.jpa.show-sql=true

    ```

3.  **Use it:**
    The application will be running on `http://localhost:8080`. You can interact with the API using a tool like **Postman**. For instance, to view all users, send a GET request to `http://localhost:8080/api/users`. You can also access the H2 database console in your browser at `http://localhost:8080/h2-console` to visually inspect the database or execute SQL queries.

## How It Works (The Smart Parts)

### How It Handles Different Currencies

The application employs a smart approach to manage expenses in various currencies (e.g., USD, INR, EUR). To maintain consistency and accuracy in calculations, it internally converts all expenses to a base currency, **US Dollars (USD)**.

Here's the workflow:
1.  A user adds an expense in their local currency (e.g., 5000 INR).
2.  The system instantly converts this amount to USD for internal processing.
3.  It then calculates each group member's share in USD.
4.  Finally, each person's balance is converted back to their preferred currency before being stored.

This ensures that all calculations are accurate and users see their balances in a familiar currency.

## Important Things to Know

Here are some key aspects of the current implementation:

*   **Fixed Exchange Rates**: The currency exchange rates are currently hard-coded. For a real-world application, these should be fetched from a live currency exchange service to reflect real-time values.

*   **Calculates Balances for ALL Groups at Once**: A known issue is that balance calculations currently consider all expenses across all groups, rather than for a specific group. This is a bug that needs to be addressed to ensure balances are calculated correctly on a per-group basis.

*   **Safe Transactions**: When an expense is added, the application updates the balances of multiple users in a single, atomic operation. Spring Boot's `@Transactional` annotation is used to manage these database operations, ensuring that they are executed safely and correctly. If any part of the process fails, the entire transaction is rolled back, preventing any inconsistencies in user balances. This is a crucial feature for maintaining data integrity.
