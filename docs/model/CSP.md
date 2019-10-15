# Cutting Stock Problem(CSP)

## 1 Problem
* A set $K$ of rolls of length $L$ is available
* Given $d \in \mathbb{Z}_+^n$ and $s \in \mathbb{R}_+^n$, the problem is to obtain $d_i$ pieces of length $s_i$ for $i \in I$, by cutting up the smallest possible number of rolls


## 2 IP Formulation
* $y_k \in \{0,1\}, k \in K, y_k = 1$ if roll $k$ is used, 0 otherwise
* $x_{ik} \geq 0 \, integer, k \in K, i \in I,$ is the number of times item $i$ being cut in roll $k$

$$
\begin{align}
z(CSP) = \min &\sum_{k \in K} y_k \\
&\sum_{k \in K} x_{ik} \geq d_i \quad (\forall i \in I) \\
&\sum_{i \in I} s_i x_{ik} \leq L y_k \quad (\forall k \in K) \\
&x \in \mathbb{Z}_+^n, \quad y \in \{ 0,1 \}^{|K|}
\end{align}
$$


## 3 Reformulation
### 3.1 Master Problem
* $v_p, p \in P$ is the number of rolls cut using pattern $p$
* $x_i^p \geq 0 \, integer, i \in I, p \in P$ is the number of items $i$ in pattern $p$

$$
\begin{align}
\min \quad &\sum_{p \in P} v_p \\
&\sum_{p \in P} x_i^p v^p \geq d_i \quad (\forall i \in I) \\
&v_p \geq 0, \, integer \quad (\forall p \in P)
\end{align}
$$

### 3.2 Pricing Problem
* $\pi_i$ is the dual price of constraints $i$ (item $i$ pieces demand)
* $x_i^p \geq 0 \, integer, i \in I, p \in P$ is the number of items $i$ in pattern $p$

$$
\begin{align}
\min \limits_{p \in P} \quad &1 - \sum_{i \in I} \pi_i x_i^p \\
&\sum_{i \in I} s_i x_i^p \leq L \\
&x_i^p \geq 0 \, integer \quad (\forall i \in I, p \in P)
\end{align}
$$
 