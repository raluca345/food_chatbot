# FoodGPT

Don't know what to eat today and would rather avoid eating cup noodles for the 4th day in a row? FoodGPT is an assistant created to help with all your culinary needs! Powered by AI, FoodGPT will answer all your food-related questions, help you find a recipe using the ingredients from your pantry and even generate images if you have an idea for a new recipe and would like to visualize the result. It also includes gallery and history features for revisiting past ideas.

A web app built with Spring Boot and React, using a MySQL database.

## Installation
For the React frontend, navigate to the `frontend` folder and install the NodeJS dependencies.

```bash
cd frontend
npm install
```

For the Spring Boot backend, navigate to the backend folder and install the Maven dependencies.

```bash
cd backend
mvn clean install
```

## Configuration

Generated images are stored in a Cloudflare R2 bucket.  
The AI integration is implemented using **Spring AI**. This project uses **Azure OpenAI** by default, but any supported model provider can be configured (see the [official documentation](https://docs.spring.io/spring-ai/reference/api/chatmodel.html)).

The database is **MySQL**, but other databases can be used with minimal configuration changes.

To configure the application, create the following files based on the provided templates:

- `application.properties` → from `application.properties.template`
- `application.yml` → from `application.yml.template`


## Running the application

### Backend

```bash
mvn spring-boot:run
```

### Frontend

```bash
npm run dev
```
