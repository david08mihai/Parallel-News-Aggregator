# Parallel News Aggregator & Analytics System

## Overview
This project is a high-performance Java-based news aggregator designed to process, deduplicate, and analyze large volumes of news articles in JSON format. By utilizing a parallel processing architecture, the system efficiently handles data ingestion, multi-criteria classification, and keyword extraction.

## Technical Features
* **Parallel Processing:** Implements a fixed-size thread pool established at initialization to maximize hardware utilization.
* **Deduplication Engine:** A two-pass parallel algorithm that identifies and removes articles with duplicate UUIDs or titles to ensure data integrity.
* **Multi-Criteria Classification:** Organizes processed articles by language and category based on predefined filter lists.
* **Global Statistics:** Generates comprehensive reports including top authors, dominant languages, and keyword frequency using thread-safe aggregation.

## Parallel Strategy
The system follows a synchronized pipeline:
1. **Parallel Ingestion:** Articles are partitioned ($N/P$) and read simultaneously by worker threads.
2. **Synchronization:** Uses `ConcurrentMap` for shared data structures and `synchronized` blocks for critical list updates to prevent race conditions.
3. **Reduction Phase:** Local statistics from each thread are merged into global maps for final reporting.
4. **Deterministic Sorting:** Final outputs are sorted lexicographically or chronologically to ensure consistent results across runs.

## Build & Dependency Management

### Using Maven
If you prefer Maven for managing dependencies (like the Jackson JSON library), run:
```bash
mvn clean install
```

### Using Makefile

For manual builds, go to src/ folder and run the following command:

```bash
make build
```

The system is controlled via CLI arguments: java NewsAggregator <num_threads> <articles_file> <aux_file>. To simplify testing with relative paths, the following Makefile targets are provided: The number of threads could be changed in the Makefile and it is set with a default value - 4.

Run Test X - where X is an integer between 1 and 5.
```bash
make testX # uses first dataset
```

Custom Run: make run ARGS="4 tests/custom/articles.txt tests/custom/inputs.txt"


### Performance & Scalability
The system was benchmarked on an Apple M4 CPU (4 Performance cores, 6 Efficiency cores) with 16 GB RAM.


| Nr. Threads | Avg Time (s) | SpeedUp (S(p)) | Efficiency (E(p)) |
|------------|--------------|---------------|------------------|
| 1  | 10.41 | 1.00x | 1.00 |
| 2  | 2.99  | 3.48x | 1.74 |
| 3  | 2.50  | 4.15x | 1.38 |
| 4  | 2.19  | 4.74x | 1.19 |
| 5  | 2.16  | 4.81x | 0.96 |
| 6  | 2.13  | 4.88x | 0.81 |
| 7  | 2.24  | 4.64x | 0.66 |
| 8  | 2.08  | 5.00x | 0.63 |
| 9  | 2.17  | 4.79x | 0.53 |
| 10 | 2.17  | 4.79x | 0.48 |

<img width="600" height="370" alt="image" src="https://github.com/user-attachments/assets/46eb519b-1dcb-4b89-98cf-a0079e5435de" />

# Scalability Insights

Performance (SpeedUp) increases near-linearly up to 4 threads, followed by a plateau, although an absolute peak is reached at 8 threads. Beyond this point, efficiency drops drastically, as the marginal speed gains no longer justify the additional resource consumption.

Hardware Constraints (P-cores vs. E-cores)

The scaling limit at 4 threads is explained by the processor's hybrid architecture. The Apple M4 features 4 high-performance cores (P-cores). Once these are saturated, the system allocates efficiency cores (E-cores), which possess significantly lower computational throughput.

# System Bottlenecks

- I/O Bound Operations: Maximum velocity is constrained by the sequential nature of disk I/O when reading and writing a massive number of files.

- Synchronization Overhead: At higher thread counts, the cost of thread creation and management becomes significant.

- Barrier Synchronization: Load imbalance can occur if some threads finish earlier than others, leading to idle time at synchronization barriers.

### Conclusion: While the absolute best time is achieved with 8 threads, the 4-thread configuration is the optimal setup, offering the most efficient balance between performance and power consumption.
