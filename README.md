****TED Talks API****

TED Talks API is a Spring Boot application designed to manage and analyze TED Talks data. The main use is to provide a comprehensive REST API for storing, retrieving, filtering, and analyzing TED Talks information. 

Key features include CSV data import, speaker influence analysis, talk statistics, pagination support, and year-based filtering. 

The application leverages Spring WebFlux for reactive, non-blocking operations, Hibernate for database management, Spring Security for authentication and authorization, and SpringDoc OpenAPI for API documentation.


# API capabilities
- **Reactive**: Uses Spring WebFlux so it's fast and non-blocking.
- **Database**: Hibernate with JPA for smooth data handling.
- **Migrations**: Flyway keeps the DB schema in check.
- **Security**: HTTP Basic authentication with role-based access control (ADMIN and USER roles).
- **Environments**: Different setups for dev, test, staging, and prod.
- **Docs**: Swagger UI for easy API testing.
- **Validation & Errors**: Checks inputs and gives nice error messages.
- **Logging**: Adjusts based on where you're running it.

The data model is simple: a TedTalk with title, author, date, views, likes, and link. CSV import is forgiving – skips bad rows and logs issues.

Database : when the CSV file is imported the data is stored in an H2 database using Hibernate ORM. Flyway manages the database schema migrations.

# Setup instructions

Install Java 21 and Maven 3.9.9

1. git clone [https://github.com/AkhileshHiremath/tedtalkAPI.git]
   cd ted-talks-api


2. **Build it**
   mvn clean install

3. **Run it**:
   ```bash
   mvn spring-boot:run
   ```
   It's up on http://localhost:8080.

For different environments, add `-Dspring-boot.run.arguments="--spring.profiles.active=dev"` or whatever.

## Environments

We have profiles for different stages:
- **Dev**: Debug mode, H2 console, SQL logs.
- **Test**: In-memory DB for tests.
- **Acc**: Schema checks for acceptance.
- **Prod**: File DB, quiet logs, secure.

## Security

Uses HTTP Basic authentication:
- **User**: `user` / `user123` (read-only access)
- **Admin**: `admin` / `admin123` (full access including CSV import)

Add `-u username:password` to your curl commands.

## API Endpoints

Base: `http://localhost:8080/api/v1/talks`

**Authentication Required** (except health/docs):
- `POST /import` - Upload CSV (**ADMIN only**)
- `GET /{id}` - One talk (requires authentication)
- `GET /` - All talks (paged, requires authentication)
- `GET /year/{year}` - Talks by year (requires authentication)
- `GET /influence/speakers` - Top speakers (requires authentication)
- `GET /stats` - Talk count (requires authentication)

**Public Endpoints**:
- `/actuator/health` - Health check
- `/swagger-ui/index.html` - API documentation
- `/v3/api-docs` - OpenAPI specification

### Example cURL Commands:

#### 1. Import CSV File (ADMIN Role Required)
```bash
# Import CSV file (ADMIN only)
curl -u admin:admin123 -X POST \
  -F "file=@iO-Data-Java assessment-tedtalk-data.csv" \
  http://localhost:8080/api/v1/talks/import

# Response will be:
# {"message":"CSV import accepted and processed successfully","recordsImported":5440,"recordsSkipped":0,"warnings":[]}
```

#### 2. Health Check (No Authentication Required)
```bash
curl http://localhost:8080/actuator/health
```

#### 3. Get Statistics (Authentication Required)
```bash
curl -u user:user123 http://localhost:8080/api/v1/talks/stats
```

#### 4. Get All Talks with Pagination (Authentication Required)
```bash
# Get first page (default: page=0, size=20)
curl -u user:user123 http://localhost:8080/api/v1/talks

# Get specific page and size
curl -u user:user123 "http://localhost:8080/api/v1/talks?page=0&size=10"
```

#### 5. Get Talk by ID (Authentication Required)
```bash
curl -u user:user123 http://localhost:8080/api/v1/talks/1
```

#### 6. Get Talks by Year (Authentication Required)
```bash
curl -u user:user123 http://localhost:8080/api/v1/talks/year/2020
```

#### 7. Get Most Influential Speakers (Authentication Required)
```bash
# Get top 5 speakers (default)
curl -u user:user123 http://localhost:8080/api/v1/talks/influence/speakers

# Get top 10 speakers
curl -u user:user123 "http://localhost:8080/api/v1/talks/influence/speakers?limit=10"
```

#### 8. Import CSV File (ADMIN Role Required)
```bash
# Import CSV file (ADMIN only)
curl -u admin:admin123 -X POST \
  -F "file=@iO-Data-Java assessment-tedtalk-data.csv" \
  http://localhost:8080/api/v1/talks/import

# Response will be:
# {"message":"CSV import accepted and processed successfully","recordsImported":5440,"recordsSkipped":0,"warnings":[]}
```

#### 9. Access Swagger UI (No Authentication Required)
```bash
# Open in browser
open http://localhost:8080/webjars/swagger-ui/index.html

# Or get API documentation
curl http://localhost:8080/v3/api-docs
```


## Testing

### Running Tests
- **Unit tests**: `mvn test`
- **With coverage**: `mvn clean test jacoco:report`
- **Quick coverage check**: `./run-coverage-tests.sh`
- **With profile**: `mvn test -Dspring.profiles.active=test`
- **Manual**: Run dev mode, hit H2 at /h2-console, or Swagger at /swagger-ui.

### Code Coverage
The project maintains **87%+ code coverage** with comprehensive unit tests.

**Coverage Stats:**
- Instruction Coverage: ≥87%
- Branch Coverage: ≥87%
- Total Tests: 228+ (including 31 security tests)

**View Coverage Report:**
```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

## How It's Built

- Controllers: Reactive with WebFlux.
- Services: Business logic, reactive.
- Repos: JPA with Hibernate.
- Security: Spring Security with HTTP Basic auth and role-based access.
- Database: H2 + Flyway.
- API Docs: SpringDoc OpenAPI with Swagger UI.
