# Uncapacitated Facility Location Problem(UFLP)

## 1 Problem
* Given a set $J$ of warehouses and a set $K$ of customers
* $f_j, j \in J$ is the fixed cost of opening warehouse $j$
* $c_{jk}, j \in J, k \in K$ is the cost of shipping commodity from warehouse $j$ to customer $k$
* Choose which warehouses to open and which open warehoues to use to supply the customers to at minimum cost

## 2 IP Formulation
* $y_j \in \{0,1\}, j \in J, y_j = 1$ if warehouse $j$ is open, 0 otherwise
* $x_{jk} \in \{0,1\}, j \in J, k \in K, x_{jk} = 1$ if use warehouse $j$ to supply customer $k$, 0 otherwise($x$ can be relaexed to $0 \leq x \leq 1$ in UFLP)

$$
\begin{align}
\min \quad &\sum_{j \in J} \sum_{k \in K} c_{jk} x_{jk} + \sum_{j \in J} f_j y_j \\
&\sum_{j \in J} x_{jk} = 1 &(\forall k \in K) \\
&x_{jk} \leq y_j &(\forall j \in J, k \in K) \\
&0 \leq x_{jk} \leq 1 &(\forall j \in J, k \in K) \\
&y_j \in \{0,1\} &(\forall j \in J)
\end{align}
$$


## 3 Reformulation
### 3.1 SubProblem - $LP(y)$
* Fix $y$, we can get $LP(y)$:

$$
\begin{align}
\quad z_{LP(y)} = \min \quad &\sum_{j \in J} \sum_{k \in K}c_{jk} x_{jk} \\
&\sum_{j \in J}x_{jk} = 1 &(\forall k \in K) \\
&x_{jk} \leq y_j &(\forall j \in J, k \in K) \\
&0 \leq x_{jk} \leq 1 &(\forall j \in J, k \in K)
\end{align}
$$


### 3.2 SubProblems - $z_{LP(y)}^k$
* $LP(y)$ can be decomposed into $|K|$ subproblems, one each for $k \in K$
* $z_{LP(y)} = \sum_{k \in K}z_{LP(y)}^k$

$$
\begin{align}
z_{LP(y)}^k = \min \quad &\sum_{j \in J}c_{jk} x_{jk} \\
&\sum_{j \in J}x_{jk} = 1 \\
&x_{jk} \leq y_j &(\forall j \in J) \\
&0 \leq x_{jk} \leq 1 &(\forall j \in J)
\end{align}
$$






