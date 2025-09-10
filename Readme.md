# Cooperative Multitasking OS Simulator

## Overview
This project is a **Java-based simulation of a simple operating system**.  
It demonstrates how an OS manages multiple programs on a single CPU through **cooperative multitasking** and **round-robin scheduling**.

Key ideas illustrated:
- Kernel vs. userland separation
- Process Control Blocks (PCBs)
- Context switching with a simulated timer interrupt
- Cooperative multitasking using semaphores
- Fair scheduling with a round-robin scheduler

---

## Architecture

### Kernel and Userland
- **Userland processes** represent programs like `HelloWorld` or `GoodbyeWorld`. They run in “normal mode” and cannot directly access kernel functions.
- **Kernel** runs in “privileged mode” and manages scheduling, process creation, and task switching.
- **OS** acts as the bridge between userland and kernelland, simulating **soft interrupts**. Userland calls OS functions, which signal the kernel to act.

### Processes
Every process:
- Has its own **thread** managed in Java.
- Is controlled by a **semaphore** that starts/stops execution.
- Calls `cooperate()` to yield control when its time quantum has expired.

### Scheduler
- Maintains a queue of **PCBs**, each wrapping a userland process.
- Uses a **Java Timer** to simulate a hardware timer interrupt every 250 ms.
- On each tick, the running process is marked as expired. At the next `cooperate()`, control switches to the next process in the queue.
- Implements **round-robin scheduling** for fairness.

### PCB (Process Control Block)
- Assigns unique process IDs (PIDs).
- Tracks priority (unused in this version).
- Provides kernel-side wrappers for process operations: start, stop, requestStop, isDone.

---

## File Breakdown
- **Process.java** – Abstract base class for all processes. Handles thread/semaphore mechanics and cooperative multitasking logic.
- **UserlandProcess.java** – Marker subclass for userland processes.
- **HelloWorld.java** – Prints `"Hello World"` in an infinite loop.
- **GoodbyeWorld.java** – Prints `"Goodbye World"` in an infinite loop.
- **IdleProcess.java** – Infinite loop that just sleeps and cooperates, used when nothing else is runnable.
- **Init.java** – The bootstrap process. Creates HelloWorld and GoodbyeWorld.
- **Main.java** – Entry point. Starts the system with `Init`.
- **OS.java** – The bridge layer that simulates interrupts between userland and kernelland.
- **Kernel.java** – The privileged component. Executes system calls and delegates scheduling.
- **Scheduler.java** – Round-robin scheduler with a timer interrupt.
- **PCB.java** – Process Control Block, the kernel’s secure handle to userland processes.
- **KernelMessage.java** – Placeholder for future inter-process communication.

---

## Execution Flow
1. **Main** starts the system by launching `Init`.
2. **Init** creates `HelloWorld` and `GoodbyeWorld` processes.
3. **Scheduler** enforces a 250 ms quantum. When the timer fires, it signals the running process to yield.
4. **Processes** run until they call `cooperate()`. If their quantum has expired, control switches to the kernel, which rotates to the next process.
5. **Console output** shows alternating bursts of `"Hello World"` and `"Goodbye World"`, demonstrating time-sharing on one CPU.

---

