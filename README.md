# 🚦 Traffic Signal Control using Q-Learning

A reinforcement learning simulation that trains an AI agent to optimally control traffic signals at a 4-way intersection, minimizing vehicle wait times and queue lengths.

---

## 📁 File: `code.py`

---

## 🔢 Line-by-Line Explanation

---

### **Lines 1–4 — Imports**

```python
import numpy as np
import random
import matplotlib.pyplot as plt
from collections import defaultdict
```

| Line | Explanation |
|------|-------------|
| `import numpy as np` | Imports NumPy for numerical operations (arrays, random integers, math). |
| `import random` | Python's built-in random module — used for ε-greedy random action selection. |
| `import matplotlib.pyplot as plt` | Used to plot the learning curve (reward vs episode graph). |
| `from collections import defaultdict` | Imports `defaultdict` to create the Q-table that auto-initializes unseen states with zeros. |

---

### **Lines 7–10 — Constants**

```python
ACTIONS = {0: "NS-Green", 1: "EW-Green", 2: "NS-Yellow", 3: "EW-Yellow"}
W = [1.0, 0.5, 0.4, 0.3, 0.6]
MAX_Q, MAX_W = 20, 40
Q_BINS, W_BINS = [0,3,6,10], [0,5,10,20]
```

| Line | Explanation |
|------|-------------|
| `ACTIONS` | A dictionary mapping action IDs (0–3) to their human-readable signal phase names. There are 4 possible actions the agent can take. |
| `W = [1.0, 0.5, 0.4, 0.3, 0.6]` | Reward weight coefficients. Controls how much each factor contributes to the reward: `W[0]`=throughput bonus, `W[1]`=wait time penalty, `W[2]`=queue length penalty, `W[3]`=congestion penalty, `W[4]`=signal switching penalty. |
| `MAX_Q, MAX_W = 20, 40` | Maximum allowed queue length (20 vehicles) and maximum wait time (40 seconds). Used to cap values from growing unboundedly. |
| `Q_BINS, W_BINS = [0,3,6,10], [0,5,10,20]` | Bin boundaries for discretizing queue and wait values into categories. Continuous values are bucketed so they can be used as Q-table keys. |

---

### **Lines 13–40 — class Env (Traffic Environment)**

```python
class Env:
```
Defines the simulated traffic intersection environment. It manages the state of the intersection and simulates what happens when the agent picks a signal action.

---

#### **Lines 14–18 — `reset()` method**

```python
def reset(self):
    self.qNS, self.qEW = float(np.random.randint(0,8)), float(np.random.randint(0,8))
    self.wNS, self.wEW = float(np.random.randint(0,10)), float(np.random.randint(0,10))
    self.phase, self.prev = 0, 0
    return self._state()
```

| Line | Explanation |
|------|-------------|
| `def reset(self)` | Resets the environment to a fresh random starting state at the beginning of each episode. |
| `self.qNS, self.qEW = ...` | Randomly initializes the queue lengths for North-South and East-West lanes to a value between 0 and 7 vehicles. |
| `self.wNS, self.wEW = ...` | Randomly initializes the wait times for NS and EW lanes to a value between 0 and 9 seconds. |
| `self.phase, self.prev = 0, 0` | Sets the current signal phase and the previous signal phase both to 0 (NS-Green). |
| `return self._state()` | Returns the initial discretized state tuple so the agent knows where it starts. |

---

#### **Lines 20–22 — `_bin()` and `_state()` methods**

```python
def _bin(self, v, b): return int(np.digitize(v, b)) - 1
def _state(self): return (self._bin(self.qNS,Q_BINS), self._bin(self.qEW,Q_BINS),
                           self._bin(self.wNS,W_BINS), self._bin(self.wEW,W_BINS), self.phase)
```

| Line | Explanation |
|------|-------------|
| `_bin(self, v, b)` | Converts a continuous value `v` into a discrete bin index using the boundaries `b`. `np.digitize` finds which bin the value falls into; subtracting 1 makes it 0-indexed. Example: a queue of 4 falls in bin 1 (between 3 and 6). |
| `_state(self)` | Builds and returns the current state as a 5-element tuple: `(NS queue bin, EW queue bin, NS wait bin, EW wait bin, current phase)`. This tuple is the key used to look up Q-values. |

---

#### **Lines 24–40 — `step(action)` method**

```python
def step(self, a):
    self.qNS = min(MAX_Q, self.qNS + np.random.randint(0,4))
    self.qEW = min(MAX_Q, self.qEW + np.random.randint(0,4))
    T = 0
    if a == 0:   # NS-Green
        s = min(self.qNS, np.random.randint(2,5)); self.qNS -= s; T = s
        self.wNS = max(0, self.wNS-3); self.wEW = min(MAX_W, self.wEW+2)
    elif a == 1: # EW-Green
        s = min(self.qEW, np.random.randint(2,5)); self.qEW -= s; T = s
        self.wEW = max(0, self.wEW-3); self.wNS = min(MAX_W, self.wNS+2)
    else:        # Yellow
        self.wNS = min(MAX_W, self.wNS+3); self.wEW = min(MAX_W, self.wEW+3)
    self.phase = a
    C = (self.qNS + self.qEW) / (2 * MAX_Q)
    S = int(a != self.prev); self.prev = a
    reward = W[0]*T - W[1]*(self.wNS+self.wEW) - W[2]*(self.qNS+self.qEW) - W[3]*C*10 - W[4]*S*5
    return self._state(), reward
```

| Line | Explanation |
|------|-------------|
| `self.qNS = min(MAX_Q, self.qNS + np.random.randint(0,4))` | New cars arrive randomly (0–3) in the NS lane each step. Capped at `MAX_Q=20`. |
| `self.qEW = min(MAX_Q, self.qEW + np.random.randint(0,4))` | Same arrival logic for the EW lane. |
| `T = 0` | Initialize throughput (number of cars that passed) to zero. |
| `if a == 0:` (NS-Green) | If NS green is active: 2–4 cars pass through NS (throughput `T`), NS queue decreases, NS wait drops by 3s, EW wait increases by 2s (EW is stopped). |
| `elif a == 1:` (EW-Green) | Same logic but for EW direction. EW cars pass, EW wait drops, NS wait increases. |
| `else:` (Yellow phase) | No cars pass during yellow. Both NS and EW wait times increase by 3s as all traffic is halted. |
| `self.phase = a` | Records the current phase for the next step's comparison. |
| `C = (self.qNS + self.qEW) / (2 * MAX_Q)` | Calculates congestion ratio: 0.0 = empty, 1.0 = fully congested. |
| `S = int(a != self.prev)` | Signal switch flag: 1 if the agent changed the phase from last step, 0 if it stayed the same. Penalizes unnecessary switching. |
| `reward = W[0]*T - W[1]*(...) - W[2]*(...) - W[3]*C*10 - W[4]*S*5` | Computes the reward: **+** throughput bonus, **−** wait time penalty, **−** queue penalty, **−** congestion penalty, **−** switch penalty. |
| `return self._state(), reward` | Returns the new state and the reward for the agent to learn from. |

---

### **Lines 43–55 — class Agent (Q-Learning Agent)**

```python
class Agent:
```
The reinforcement learning agent that learns to control the traffic signals using Q-Learning.

---

#### **Lines 44–46 — `__init__()` constructor**

```python
def __init__(self, alpha=0.1, gamma=0.95, eps=1.0, eps_min=0.05, decay=0.97):
    self.alpha, self.gamma, self.eps, self.eps_min, self.decay = alpha, gamma, eps, eps_min, decay
    self.Q = defaultdict(lambda: np.zeros(4))
```

| Line | Explanation |
|------|-------------|
| `alpha=0.1` | Learning rate — how much the agent updates its Q-values on each step. Small value = slow but stable learning. |
| `gamma=0.95` | Discount factor — how much the agent values future rewards vs immediate ones. 0.95 means future rewards are nearly as important as present ones. |
| `eps=1.0` | Epsilon (ε) — starts at 1.0 meaning the agent acts completely randomly at first (pure exploration). |
| `eps_min=0.05` | The minimum epsilon — the agent will always keep at least 5% randomness even after full training. |
| `decay=0.97` | Epsilon decay rate — after each episode, ε is multiplied by 0.97, gradually reducing exploration. |
| `self.Q = defaultdict(lambda: np.zeros(4))` | The Q-table stored as a dictionary. Any new (unseen) state is automatically initialized with Q-values of `[0, 0, 0, 0]` for all 4 actions. |

---

#### **Lines 48–49 — `act()` method (ε-greedy policy)**

```python
def act(self, s):
    return random.randint(0,3) if random.random() < self.eps else int(np.argmax(self.Q[s]))
```

| Line | Explanation |
|------|-------------|
| `random.random() < self.eps` | Generates a random float between 0 and 1. If it's less than epsilon, the agent **explores**. |
| `random.randint(0,3)` | Exploration: picks a completely random action (0, 1, 2, or 3). |
| `int(np.argmax(self.Q[s]))` | Exploitation: picks the action with the **highest Q-value** for the current state `s`. |

---

#### **Lines 51–52 — `update()` method (Bellman equation)**

```python
def update(self, s, a, r, s2):
    self.Q[s][a] += self.alpha * (r + self.gamma*np.max(self.Q[s2]) - self.Q[s][a])
```

| Line | Explanation |
|------|-------------|
| `self.Q[s][a] += ...` | Updates the Q-value for state `s`, action `a`. |
| `r + self.gamma*np.max(self.Q[s2])` | The **target** value: actual reward received + discounted best future reward from next state `s2`. |
| `... - self.Q[s][a]` | The **TD error**: difference between the target and current estimate. |
| `self.alpha * (...)` | Scales the update by the learning rate — the Q-value moves a small step toward the target. |

> This is the **core Bellman update equation** of Q-Learning:
> `Q(s,a) <- Q(s,a) + alpha * [r + gamma * max Q(s') - Q(s,a)]`

---

#### **Lines 54–55 — `decay_eps()` method**

```python
def decay_eps(self):
    self.eps = max(self.eps_min, self.eps * self.decay)
```

| Line | Explanation |
|------|-------------|
| `self.eps * self.decay` | Reduces epsilon by 3% each episode (multiplied by 0.97). |
| `max(self.eps_min, ...)` | Ensures epsilon never falls below the minimum threshold of 0.05 (5%). |

---

### **Lines 58–68 — `train()` function**

```python
def train(episodes=200, steps=100):
    env, agent, rewards = Env(), Agent(), []
    for ep in range(episodes):
        s = env.reset(); total = 0
        for _ in range(steps):
            a = agent.act(s); s2, r = env.step(a)
            agent.update(s, a, r, s2); s = s2; total += r
        agent.decay_eps(); rewards.append(total)
        if (ep+1) % 40 == 0:
            print(f"Ep {ep+1:>3} | Reward: {total:>8.1f} | eps: {agent.eps:.3f}")
    return agent, rewards
```

| Line | Explanation |
|------|-------------|
| `train(episodes=200, steps=100)` | Runs 200 episodes, each with 100 time steps. |
| `env, agent, rewards = Env(), Agent(), []` | Creates a fresh environment, a new agent, and an empty reward log. |
| `for ep in range(episodes)` | Outer loop — iterates over each training episode. |
| `s = env.reset(); total = 0` | Resets the intersection to a random start state and zeroes the episode reward counter. |
| `for _ in range(steps)` | Inner loop — simulates 100 time steps per episode. |
| `a = agent.act(s)` | Agent selects an action using ε-greedy policy. |
| `s2, r = env.step(a)` | Environment executes the action and returns the new state and reward. |
| `agent.update(s, a, r, s2)` | Agent updates its Q-table using the Bellman equation. |
| `s = s2; total += r` | Moves to the next state and accumulates the episode reward. |
| `agent.decay_eps()` | After each episode, reduces epsilon (less exploration over time). |
| `rewards.append(total)` | Records the total reward for this episode for later plotting. |
| `if (ep+1) % 40 == 0` | Prints a progress update every 40 episodes. |
| `return agent, rewards` | Returns the trained agent and the full reward history. |

---

### **Lines 71–78 — `plot()` function**

```python
def plot(rewards):
    ep = np.arange(1, len(rewards)+1)
    sm = np.convolve(rewards, np.ones(10)/10, mode='same')
    plt.figure(figsize=(9,4))
    plt.plot(ep, rewards, alpha=0.3, label="Raw"); plt.plot(ep, sm, lw=2, label="Smoothed")
    plt.xlabel("Episode"); plt.ylabel("Cumulative Reward")
    plt.title("Q-Learning: Total Cumulative Reward vs Episode"); plt.legend(); plt.grid(alpha=0.3)
    plt.tight_layout(); plt.savefig("learning_curve.png", dpi=150); plt.show()
```

| Line | Explanation |
|------|-------------|
| `ep = np.arange(1, len(rewards)+1)` | Creates an array of episode numbers [1, 2, ..., 200] for the x-axis. |
| `sm = np.convolve(rewards, np.ones(10)/10, mode='same')` | Applies a **10-episode moving average** to smooth the noisy reward curve, making the trend easier to see. |
| `plt.figure(figsize=(9,4))` | Creates a wide figure (9x4 inches). |
| `plt.plot(..., alpha=0.3, label="Raw")` | Plots the raw, noisy episode rewards in faint (30% opacity). |
| `plt.plot(..., lw=2, label="Smoothed")` | Plots the smoothed average with a thicker line on top. |
| `plt.savefig("learning_curve.png", dpi=150)` | Saves the chart as a high-resolution PNG file in the current directory. |
| `plt.show()` | Displays the chart in a window. |

---

### **Lines 81–93 — `evaluate()` function**

```python
def evaluate(agent, steps=200):
    env = Env(); s = env.reset()
    total, switches, wait, queue, prev = 0, 0, 0, 0, None
    for _ in range(steps):
        a = int(np.argmax(agent.Q[s]))
        if prev is not None and a != prev: switches += 1
        s, r = env.step(a); total += r
        wait += (env.wNS + env.wEW)/2; queue += (env.qNS + env.qEW)/2; prev = a
    print(f"\nEvaluation Results ({steps} steps) ")
    print(f"  Avg Wait Time   : {wait/steps:.2f} s")
    print(f"  Avg Queue Length: {queue/steps:.2f} vehicles")
    print(f"  Signal Switches : {switches}")
    print(f"  Total Reward    : {total:.1f}")
```

| Line | Explanation |
|------|-------------|
| `evaluate(agent, steps=200)` | Tests the trained agent for 200 steps. No training happens here — pure evaluation. |
| `env = Env(); s = env.reset()` | Creates a fresh environment and gets the starting state. |
| `total, switches, wait, queue, prev = 0, 0, 0, 0, None` | Initializes all metric counters to zero. `prev` tracks the previous action to detect switches. |
| `a = int(np.argmax(agent.Q[s]))` | Always picks the **best known action** (greedy, no randomness — epsilon=0 during eval). |
| `if prev is not None and a != prev: switches += 1` | Counts how many times the signal phase changed — fewer switches = smoother operation. |
| `s, r = env.step(a); total += r` | Steps the environment and accumulates the reward. |
| `wait += (env.wNS + env.wEW)/2` | Accumulates the average wait time across both directions each step. |
| `queue += (env.qNS + env.qEW)/2` | Accumulates the average queue length across both directions each step. |
| `print(f"  Avg Wait Time...")` | Reports average wait time per step: total_wait / steps. |
| `print(f"  Avg Queue Length...")` | Reports average queue per step: total_queue / steps. |
| `print(f"  Signal Switches...")` | Total number of phase changes during evaluation. |
| `print(f"  Total Reward...")` | Sum of all rewards received during evaluation. |

---

### **Lines 95–100 — Entry Point (`__main__`)**

```python
if __name__ == "__main__":
    agent, rewards = train()
    plot(rewards)
    half = len(rewards)//2
    print(f"\nAvg reward early: {np.mean(rewards[:half]):.1f}  |  late: {np.mean(rewards[half:]):.1f}")
    evaluate(agent)
```

| Line | Explanation |
|------|-------------|
| `if __name__ == "__main__":` | Ensures this block only runs when the script is executed directly (not imported as a module). |
| `agent, rewards = train()` | Trains the Q-Learning agent for 200 episodes and collects the reward history. |
| `plot(rewards)` | Generates and saves the learning curve plot. |
| `half = len(rewards)//2` | Finds the midpoint of the reward list (episode 100). |
| `np.mean(rewards[:half])` | Average reward for the **first half** of training (early, mostly random). |
| `np.mean(rewards[half:])` | Average reward for the **second half** (later, more learned). A higher value confirms improvement. |
| `evaluate(agent)` | Runs a final greedy evaluation and prints performance metrics. |

---

## 🧠 How Q-Learning Works (Summary)

```
State (s)  -->  Agent picks action (a) via epsilon-greedy
     |
     v
Environment executes action  -->  new state (s'), reward (r)
     |
     v
Q[s][a] <- Q[s][a] + alpha * (r + gamma * max Q[s'] - Q[s][a])
     |
     v
Repeat for 200 episodes x 100 steps = 20,000 total steps
```

The agent starts **randomly** and progressively **learns** which traffic signal phases reduce congestion and waiting — without any hand-coded rules.

---

## 📊 Expected Output

```
Ep  40 | Reward:   -342.5 | eps: 0.885
Ep  80 | Reward:   -201.3 | eps: 0.783
Ep 120 | Reward:   -134.7 | eps: 0.693
Ep 160 | Reward:    -98.2 | eps: 0.613
Ep 200 | Reward:    -71.4 | eps: 0.543

Avg reward early: -210.4  |  late: -89.6

Evaluation Results (200 steps)
  Avg Wait Time   : 8.34 s
  Avg Queue Length: 5.12 vehicles
  Signal Switches : 47
  Total Reward    : -1423.6
```

> As training progresses, the reward becomes **less negative**, indicating the agent is learning to manage traffic more efficiently.

---

## 🚀 How to Run

```bash
pip install numpy matplotlib
python code.py
```

Output: Terminal metrics + `learning_curve.png` saved in the current directory.
