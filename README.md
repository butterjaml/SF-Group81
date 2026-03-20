# SF-Group81 - TA Management System

## Project Setup
- Java 17
- Maven
- Swing desktop client
- CSV-based persistence in `data/`

## Run
```bash
mvn -q -DskipTests compile
mvn test
mvn -q exec:java -Dexec.mainClass="com.sfgroup81.tams.App"
```

## Branch Rule
Follow `docs/branching-strategy.md`.
