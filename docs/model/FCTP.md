# Fixed Charge Transportation Problem(FCTP)

## 1 Problem
* Given a set $J$ of warehouses and a set of $K$ customers
* $f_j, j \in J$ is the fixed cost of opening warehouse $j$
* $s_j, j \in J$ is the capacity of warehouse $j$
* $d_k, k \in K$ is the demand of customer $k$
* $c_{jk}, j \in J, k \in K$ is the unit cost of shipping commodity from warehouse $j$ to customer $k$
* Choose which warehouses to open and the flow from open warehoues to customers to satisfy some fixed demand at minimum cost

## 2 IP Formulation
* $y_j \in \{0,1\}, j \in J, y_j = 1$ if warehouse $j$ is open, 0 otherwise
* $x_{jk} \geq 0, j \in J, k \in K$ is the flow from warehouse $j$ to customer $k$

$$
\begin{align}
\min \quad &\sum_{j \in J} \sum_{k \in K} c_{jk} x_{jk} + \sum_{j \in J} f_j y_j \\
&\sum_{j \in J} x_{jk} \geq d_k &(\forall k \in K) \\
&\sum_{k \in K} x_{jk} \leq s_j y_j &(\forall j \in J) \\
&x_{jk} \geq 0 &(\forall j \in J, k \in K) \\
&y_j \in \{0,1\} &(\forall j \in J)
\end{align}
$$


## 3 Reformulation
### 3.1 Primal SubProblem - $LP(y)$
* Fix $y$, we can get $LP(y)$:

$$
\begin{align}
\quad z_{LP(y)} = min \quad &\sum_{j \in J}\sum_{k \in K}c_{jk} x_{jk} \tag{3.1.1} \\
&\sum_{j \in J}x_{jk} \geq d_k &(\forall k \in K) \tag{3.1.2} \label{3.1.2} \\
&\sum_{k \in K} x_{jk} \leq s_j y_j &(\forall j \in J) \tag{3.1.3} \label{3.1.3} \\
&x_{jk} \geq 0 &(\forall j \in J, k \in K)
\end{align}
$$

### 3.2 Dual SubProblem
* $u_k \geq 0, k \in K$ is the dual variables corresponding to $\ref{3.1.2}$
* $-v_j \leq 0, j \in J$ is the dual variables corresponding to $\ref{3.1.3}$

$$
\begin{align}
\quad z_{LP(y)} = \max \quad &\sum_{k \in K} d_k u_k - \sum_{j \in J} s_j v_j y_j \\
&u_k - v_j \leq c_{jk} \quad (\forall j \in J, k \in K)  \tag{3.2.1} \label{3.2.1} \\
&u,v \geq 0
\end{align}
$$


### 3.3 Master Problem
* $p \in P$ is the extreme point corresponding to $\ref{3.2.1}$
* $r \in R$ is the extreme direction corresponding to $u_k - v_j \leq 0$

$$
\begin{align}
\min \quad &\sum_{j \in J} f_j y_j + \sigma \\
&\sum_{k \in K} d_k u_k^p - \sum_{j \in J} s_j v_j^p y_j \leq \sigma &(\forall p \in P) \\
&\sum_{k \in K} d_k u_k^r - \sum_{j \in J} s_j v_j^r y_j \leq 0 &(\forall r \in R)
\end{align}
$$

