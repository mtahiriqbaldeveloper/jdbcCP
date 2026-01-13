# jdbcCP - Java Database Connection Pool

A high-performance, thread-safe JDBC connection pool implementation with advanced features including leak detection, statement caching, and configurable entity management.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Advanced Features](#advanced-features)
- [Testing](#testing)
- [License](#license)

## Overview

jdbcCP is a production-ready JDBC connection pool that provides efficient database connection management with built-in monitoring, leak detection, and connection validation. It's built on a generic `EntityPool` framework that can be used to pool any type of resource, not just database connections.

## Features

### Core Features
- ✅ **Thread-Safe Connection Pooling** - Concurrent access with semaphore-based permit management
- ✅ **Dynamic Pool Sizing** - Configurable initial, max, and priority pool sizes
- ✅ **Connection Validation** - Automatic connection health checks using configurable checkers
- ✅ **Leak Detection** - Built-in connection leak detection with stack trace reporting
- ✅ **Statement Caching** - Optional prepared statement caching for improved performance
- ✅ **Timeout Management** - Configurable checkout timeouts with priority handling
- ✅ **Parallel Initialization** - Concurrent connection creation during pool startup
- ✅ **Automatic Refresh** - Connection validation and eviction of invalid connections
- ✅ **Graceful Shutdown** - Proper resource cleanup on pool closure

### Advanced Features
- 🔍 **Connection Leak Detection** - Automatically detects connections held beyond threshold (default: 50 seconds)
- 📊 **Pool State Debugging** - Built-in diagnostic methods for monitoring pool health
- 🔄 **Entity Converters** - Transform connections with custom decorators
- 🎯 **Priority Access** - Support for prioritized connection requests
- 📝 **Comprehensive Logging** - SLF4J-based logging with detailed diagnostics

## Requirements

- **Java**: 8 or higher
- **Build Tool**: Gradle 6.0+
- **Database**: Any JDBC-compliant database (PostgreSQL example provided)

### Dependencies

```gradle
dependencies {
    implementation 'org.postgresql:postgresql:42.7.3'
    implementation 'org.slf4j:slf4j-api:2.0.17'
    implementation 'org.slf4j:slf4j-log4j12:2.0.17'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}
```

## Installation

### Using Gradle

1. Clone the repository:
```bash
git clone https://github.com/yourusername/jdbcCP.git
cd jdbcCP
```

2. Build the project:
```bash
./gradlew build
```

3. Run tests:
```bash
./gradlew test
```

## Quick Start

### Basic Usage

```java
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

public class Example {
    public static void main(String[] args) throws SQLException {
        // Configure properties
        Properties properties = new Properties();
        properties.setProperty("user", "your_username");
        properties.setProperty("password", "your_password");
        
        // Build the connection pool
        JdbcPoolBuilder builder = new JdbcPoolBuilder(properties);
        builder.setDriverClass("org.postgresql.Driver");
        builder.setUrl("jdbc:postgresql://localhost:5432/your_database");
        builder.setDefaultCheckoutTime(2000); // 2 seconds timeout
        
        JdbcPool pool = builder.build();
        DataSource dataSource = pool;
        
        // Get and use a connection
        try (Connection connection = dataSource.getConnection()) {
            // Use the connection
            // Connection is automatically returned to pool when closed
        }
        
        // Shutdown pool when done
        pool.close();
    }
}
```

### Advanced Configuration

```java
JdbcPoolBuilder builder = new JdbcPoolBuilder(properties);

// Database settings
builder.setDriverClass("org.postgresql.Driver");
builder.setUrl("jdbc:postgresql://localhost:5432/mydb");

// Pool configuration
builder.setInitialSize(5);              // Start with 5 connections
builder.setMaxSize(20);                 // Maximum 20 connections
builder.setDefaultCheckoutTime(3000);   // 3 second timeout

// Connection validation
builder.setIdleConnectionCheckPeriod(60000);  // Check every minute
builder.setAutocloseStatements(true);         // Auto-close statements

// Build the pool
JdbcPool pool = builder.build();
```

## Configuration

### JdbcPoolBuilder Options

| Method | Description | Default |
|--------|-------------|---------|
| `setUrl(String)` | JDBC connection URL | Required |
| `setDriverClass(String)` | JDBC driver class name | Required |
| `setInitialSize(int)` | Initial pool size | 5 |
| `setMaxSize(int)` | Maximum pool size | 10 |
| `setPrioritySize(int)` | Reserved connections for priority requests | 0 |
| `setDefaultCheckoutTime(long)` | Default connection checkout timeout (ms) | 2000 |
| `setIdleConnectionCheckPeriod(long)` | Idle connection check interval (ms) | 600000 |
| `setAutocloseStatements(boolean)` | Auto-close statements on connection close | true |
| `setUseConnectionStatementCache(boolean)` | Enable statement caching | true |

### EntityPool Configuration

The underlying `EntityPool` can be configured directly for custom entity pooling:

```java
EntityPool<YourType> pool = EntityPool.<YourType>builder()
    .factory(yourFactory)                    // Entity factory
    .releaser(yourReleaser)                  // Entity releaser
    .initialSize(10)                         // Initial pool size
    .maxSize(50)                             // Max pool size
    .defaultCheckoutTime(5000)               // Checkout timeout
    .prioritySize(5)                         // Priority slots
    .parrelCreation(true)                    // Parallel initialization
    .addCheckers(checker1, checker2)         // Health checkers
    .build();
```

## Architecture

### Component Overview

```
┌─────────────────────────────────────┐
│         JdbcPool (DataSource)       │
│  - Connection management interface  │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│    EntityPool<PoolConnection>       │
│  - Generic pooling implementation   │
│  - Semaphore-based permits          │
│  - Leak detection                   │
│  - Connection validation            │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│        PoolConnection               │
│  - Wrapped JDBC connection          │
│  - Statement caching                │
│  - Lifecycle management             │
└─────────────────────────────────────┘
```

### Key Classes

- **`EntityPool<T>`**: Generic resource pool implementation
- **`JdbcPool`**: JDBC-specific DataSource implementation
- **`JdbcPoolBuilder`**: Fluent builder for creating JDBC pools
- **`PoolConnection`**: Wrapper for JDBC connections with lifecycle management
- **`EntityChecker<T>`**: Interface for validating pooled entities
- **`EntityFactory<T>`**: Interface for creating new entities
- **`EntityReleaser<T>`**: Interface for releasing/destroying entities

## Advanced Features

### Leak Detection

The pool automatically monitors connections and reports leaks:

```java
// Leak detection is enabled by default
// Threshold: 50 seconds (configurable)
// Runs every 60 seconds

// When a leak is detected, you'll see:
// - Thread that checked out the connection
// - Stack trace at checkout time
// - How long the connection has been held
// - Auto-release after 2x threshold
```

### Statement Caching

Improve performance by caching prepared statements:

```java
builder.setUseConnectionStatementCache(true);

// Statements are cached per connection
// Automatically reused for same SQL
// Cleared when connection is released
```

### Connection Validation

Connections are validated before use:

```java
// Built-in DefaultConnectionChecker
// Validates connection is not closed
// Custom checkers can be added

EntityChecker<Connection> customChecker = connection -> {
    // Your validation logic
    return connection.isValid(1);
};
```

### Priority Requests

Reserve connections for high-priority operations:

```java
builder.setPrioritySize(3);  // Reserve 3 connections

// Execute with priority
EntityPoolContext.execPrioritized(() -> {
    Connection conn = dataSource.getConnection();
    // This request bypasses priority restrictions
});
```

### Pool Monitoring

```java
// Check pool state
int currentSize = pool.getPoolSize();
int idleCount = pool.getIdleEntitiesNumber();

// Debug pool state
pool.debugPoolState();
// Output:
// === Pool Debug Info ===
// Current size: 10
// Idle entities: 7
// Max pool size: 20
// Available permits: 10
```

## Testing

The project includes comprehensive tests:

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests EntityPoolBugTest

# View test reports
open build/reports/tests/test/index.html
```

### Test Coverage

- ✅ Connection pooling under concurrent load
- ✅ Leak detection functionality
- ✅ Timeout handling
- ✅ Connection validation and refresh
- ✅ Statement caching
- ✅ Pool initialization and shutdown

## Project Structure

```
jdbcCP-main/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── EntityPool.java              # Core pooling logic
│   │   │   ├── JdbcPool.java                # JDBC DataSource impl
│   │   │   ├── JdbcPoolBuilder.java         # Builder pattern
│   │   │   ├── PoolConnection.java          # Connection wrapper
│   │   │   ├── EntityPoolContext.java       # Thread context
│   │   │   ├── api/                         # Public interfaces
│   │   │   │   ├── EntityChecker.java
│   │   │   │   ├── EntityFactory.java
│   │   │   │   └── EntityReleaser.java
│   │   │   └── ...                          # Supporting classes
│   │   └── resources/
│   │       └── log4j.properties             # Logging config
│   └── test/
│       └── java/
│           └── EntityPoolBugTest.java       # Test suite
├── build.gradle                             # Build configuration
├── settings.gradle
├── LICENSE                                  # Apache 2.0
└── README.md
```

## Performance Considerations

- **Initial Pool Size**: Set based on expected baseline load
- **Max Pool Size**: Consider database connection limits and server resources
- **Checkout Timeout**: Balance between responsiveness and request success
- **Statement Caching**: Enable for applications with repetitive queries
- **Parallel Creation**: Enable for faster startup with multiple connections
- **Idle Check Period**: Tune based on database timeout settings

## Troubleshooting

### Connection Timeout
```
Problem: getConnection() returns null
Solution: Increase defaultCheckoutTime or maxPoolSize
```

### Connection Leaks
```
Problem: Pool exhausted, connections not released
Solution: Always use try-with-resources, check leak detection logs
```

### Slow Startup
```
Problem: Pool initialization takes too long
Solution: Enable parallelCreation or reduce initialSize
```

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Java naming conventions
- Add JavaDoc for public APIs
- Include unit tests for new features
- Maintain test coverage above 80%

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

```
Copyright 2024

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Support

For issues, questions, or contributions:
- 🐛 Report bugs via [GitHub Issues](https://github.com/yourusername/jdbcCP/issues)
- 💬 Ask questions via [GitHub Discussions](https://github.com/yourusername/jdbcCP/discussions)
- 📧 Contact: your.email@example.com

## Acknowledgments

- Built with modern Java concurrency utilities
- Inspired by popular connection pool implementations (HikariCP, Apache DBCP)
- Uses SLF4J for flexible logging

---

**Note**: This is a learning/demonstration project. For production use, consider battle-tested alternatives like HikariCP or Apache Commons DBCP.
