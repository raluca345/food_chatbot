# FoodGPT

Don't know what to eat today and would rather avoid eating cup noodles for the 4th day in a row? FoodGPT is an assistant created to help with all your culinary needs! Powered by AI, FoodGPT will answer all your food-related questions, help you find a recipe using the ingredients from your pantry and even generate images if you have an idea for a new recipe and would like to visualize the result. It also includes gallery and history features for revisiting past ideas.

A web app built with Spring Boot and React, using a MySQL database.


## Features 

Talk about food:

![food chat](https://github.com/user-attachments/assets/e4520996-7830-4074-b62c-b127db764b9d)  


Find what to cook today (generate a recipe with the ingredients from your pantry):

![recipe](https://github.com/user-attachments/assets/65d715a4-25f7-4750-975d-0915d73a98d1)


Bring to life your latest craving (generate an image):

![image](https://github.com/user-attachments/assets/2b2251ea-dafc-4fdf-87a1-1b1d7ba2ea8e)


View your recipes, download your favorites and delete those that didn't taste as good as expected:

![recipe history](https://github.com/user-attachments/assets/0bfc31a5-6756-427d-abd2-ffd2efcacded)


View your images, download your favorites and delete those that don't make you hungry:

![gallery](https://github.com/user-attachments/assets/d257d704-a4ee-4980-b91f-edacfc376db3)

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
