# Exact Algorithm
Exact algorithm implementation based on Cplex Java API.

## Problem and Algorithm
### Column generation
1. [Cut Stock Problem](./src/csp)
    - [Model](./docs/model/CSP.md)


### Benders Decomposition
1. [Uncapacitated Facility Location Problem(UFLP)](./src/uflp)
    - [Model](./docs/model/UFLP.md)
    - 基于 Cplex Benders Annotation 的 UFLP 的 Benders decomposition algorithm 实现

2. [Fixed Charge Transportation Problem(FCTP)](./src/fctp)
    - [Model](./docs/model/FCTP.md)
    - 基于 Cplex 通用回调（Generic Callback）实现
    - 支持原形式和对偶形式两种场景


### Branch and Bound
1. [VRPTW](./src/vrptw/algorithm/branchandbound)
    - 基于 Cplex 的 BranchCallback 实现
    - 旨在展示自定义 BranchCallback 的使用，对于代码未进行过多优化.


### Branch and Price
1. [VRPTW](./src/vrptw/algorithm/branchandprice)
    - 子问题 ESPPTWCC 使用 [Pulse Algorithm](./src/vrptw/algorithm/subproblem/pulsealgorithm) 求解
    - 旨在展示 Branch and Price Algorithm 的代码框架
    - 目前 C101-C109, R101 已经通过测试


## Test Demo
The test demo code is in [src/testdemo](./src/testdemo)


## License
The content of this project is licensed under the [Apache License 2.0](LICENSE).
