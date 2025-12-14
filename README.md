# Predictify Backend - AI-Powered Event Management System

REST API for event management with attendance predictions based on Artificial Intelligence.

## 🏗️ Architecture

- **Framework**: Spring Boot 3.4.0
- **Database**: PostgreSQL 17
- **Authentication**: JWT (JSON Web Tokens)
- **ORM**: Hibernate/JPA with UUIDs as PKs
- **Documentation**: OpenAPI/Swagger UI

## 🚀 Project Setup

### 1. Start Database

```bash
docker-compose up -d
```

This will start:
- PostgreSQL 17 on port 5435
- pgAdmin at http://localhost:5050

### 2. Run Backend

```bash
mvn spring-boot:run
```

The API will be available at: **http://localhost:8081**

### 3. API Documentation

Swagger UI: **http://localhost:8081/swagger-ui.html**

## 📡 Main Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Log in |

### Events

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/events` | List published events | Public |
| GET | `/api/events/upcoming` | Upcoming events | Public |
| GET | `/api/events/featured` | Featured events | Public |
| GET | `/api/events/trending` | Trending events | Public |
| GET | `/api/events/{slug}` | Event details | Public |
| POST | `/api/events` | Create event | ORGANIZER, ADMIN |
| PUT | `/api/events/{id}` | Update event | ORGANIZER (own), ADMIN |
| DELETE | `/api/events/{id}` | Delete event | ORGANIZER (own), ADMIN |
| POST | `/api/events/{id}/publish` | Publish event | ORGANIZER (own), ADMIN |

### Attendance Registrations

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| POST | `/api/events/{eventId}/register` | Register for event | ATTENDEE, ORGANIZER, ADMIN |
| DELETE | `/api/events/{eventId}/register` | Cancel registration | ATTENDEE, ORGANIZER, ADMIN |
| GET | `/api/events/{eventId}/registrations` | View registrations | ORGANIZER (own), ADMIN |
| POST | `/api/events/{eventId}/interested` | Mark as interested | ATTENDEE, ORGANIZER, ADMIN |

### Predictions

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/events/{id}/prediction` | Get prediction | ATTENDEE, ORGANIZER, ADMIN |
| POST | `/api/events/{id}/prediction/generate` | Generate prediction | ORGANIZER (own), ADMIN |

### Users

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/users/me` | Current user profile | Authenticated |
| PUT | `/api/users/me` | Update profile | Authenticated |
| GET | `/api/users/me/events` | My registered events | Authenticated |

### Organizers

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| POST | `/api/organizers/profile` | Create organizer profile | ORGANIZER, ADMIN |
| GET | `/api/organizers/{id}` | View organizer profile | Public |

## 🔐 Authentication

All protected endpoints require the header:

```
Authorization: Bearer {token}
```

### Login Example

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

## 👥 User Roles

### ATTENDEE (Regular User)
- View published events
- Register for events
- Save favorite events
- View predictions

### ORGANIZER (Organizer)
- Everything included in ATTENDEE
- Create and manage own events
- View analytics for own events
- Generate predictions

### ADMIN (Administrator)
- Full system access
- Manage any event
- Verify organizers
- View global analytics

## 🗄️ Database

The complete schema can be found in `src/main/resources/schema.sql`

### Main Tables
- **users**: User management
- **events**: System events
- **event_registrations**: Attendance registrations
- **event_predictions**: AI predictions
- **organizers**: Organizer profiles
- **permissions**: Permission system
- **protected_routes**: Frontend route configuration

Total: 33+ tables with complex relationships

## 🧪 Test Data

The `schema.sql` file includes:
- 14 predefined prediction factors
- 40+ system permissions
- 20+ configured protected routes

## 🌐 Angular Frontend Integration

### Required Headers

```typescript
const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${token}`
};
```

### Recommended Services

```typescript
// auth.service.ts
class AuthService {
  login(email: string, password: string): Observable<AuthResponse>
  register(data: RegisterRequest): Observable<AuthResponse>
  logout(): void
}

// events.service.ts
class EventsService {
  getUpcomingEvents(): Observable<Event[]>
  getEventBySlug(slug: string): Observable<Event>
  registerToEvent(eventId: string): Observable<void>
}

// permissions.service.ts
class PermissionsService {
  getUserPermissions(): Observable<string[]>
  hasPermission(permission: string): boolean
}
```

### Route Guards

```typescript
@Injectable()
export class AuthGuard implements CanActivate {
  canActivate(): boolean {
    return !!this.authService.getToken();
  }
}

@Injectable()
export class RoleGuard implements CanActivate {
  canActivate(route: ActivatedRouteSnapshot): boolean {
    const requiredRoles = route.data['roles'];
    return this.authService.hasRole(requiredRoles);
  }
}
```

## 📊 AI Predictions

The system uses multiple factors to predict attendance:

### Positive Factors
- High engagement (interested vs. capacity)
- Trending topic
- Verified organizer
- Early registrations
- Good organizer history

### Negative Factors
- Free event (higher no-show rate)
- Competing events on the same date
- Poor forecasted weather
- Holiday season

## 🔧 Configuration

### Environment Variables

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/predictify_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=secret

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000

# Gemini AI
GEMINI_API_KEY=your-api-key
```

## 📝 Data Models (DTOs)

### RegisterRequest
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123",
  "role": "ATTENDEE"
}
```

### EventCreateRequest
```json
{
  "title": "Angular Workshop 2024",
  "description": "Learn Angular from scratch",
  "category": "WORKSHOP",
  "type": "PRESENCIAL",
  "startDate": "2024-12-20",
  "startTime": "10:00:00",
  "capacity": 50,
  "price": 0,
  "isFree": true,
  "location": {
    "type": "PHYSICAL",
    "city": "Bogotá",
    "country": "Colombia",
    "address": "Calle 123 #45-67"
  }
}
```

## 🐛 Troubleshooting

### Database Won't Start
```bash
docker-compose down
docker volume rm predictify-backend_postgres_data_secure
docker-compose up -d
```

### Port 8081 in Use
Change in `application.yml`:
```yaml
server:
  port: 8082
```

### Compilation Fails
```bash
mvn clean install -DskipTests
```

## 📚 Tech Stack

- Java 21
- Spring Boot 3.4.0
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL 17
- Hibernate
- Lombok
- OpenAPI/Swagger
- Maven
- Docker & Docker Compose

## 🤝 Contribution

1. Fork the project
2. Create a branch for your feature (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👨‍💻 Author

**PredictifyLabs Team**

---

**Note**: This backend is designed to work with an Angular frontend that consumes these endpoints.
