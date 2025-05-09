# Intergamma stock-service assessment 

Keeps track of the stock of a product per store and allows you to reserve the stock. 

# Development instructions

### JOOQ
The database interaction code is generated via JOOQ. 
It reverse engineers the database and creates the necessary classes to interact with the database.
To generate the classes:
1. we start a docker container with a clean Postgressql database 
2. apply our flyway database scripts
3. run the jooq code generator and save the generated code in src/main/java for simplicity now.

To run the code generation, simply run:

    ./gradlew codegen. 


### Running locally

1. To run the application locally, you first need to start the database 

    docker run --name igstock --restart unless-stopped -p 9192:5432 -e POSTGRES_USER=stock -e POSTGRES_PASSWORD=stock -d postgres:16.3

2. To start the application use `./gradlew bootRun` or run StockServiceApplication from within your IDE

3. The application is now running on http://localhost:8080, if you go there you will be redirected to the swagger page where you can test the endpoints.

4. You can take a look at the Integration tests that test the complete flow at [StockServiceIntegrationTest](src/test/kotlin/net/intergamma/stock/StockServiceIntegrationTest.kt)