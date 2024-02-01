# CXGame
The ConnectX Game project is an implementation of an AI player for the ConnectX game, a variant of Connect Four. Developed in Java, it uses a minimax algorithm with alpha-beta pruning for intelligent decision-making during the game.

## Project Description
The project consists of implementing an automatic player for the ConnectX game, which is a variant of the popular Connect Four strategy game. The automatic player was developed in Java and uses a minimax search algorithm with alpha-beta pruning to make intelligent decisions during the game.

## Getting Started
To compile the project, navigate to the connectx/ directory and run the following command:
        javac -cp ".." *.java */*.java
  
### CXGame Application:
To play against the AI or watch AI vs. AI matches, use the following commands:

- Human vs Computer:
        java -cp ".." connectx.CXGame 6 7 4 connectx.L0.L0

- Computer vs Computer:
        java -cp ".." connectx.CXGame 6 7 4 connectx.L0.L0 connectx.L1.L1

### CXPlayerTester Application
Test the AI player with different configurations:

- Output score only:
        java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1

- Verbose output:
  java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1 -v
  
- Verbose output with customized timeout (1 sec) and number of game repetitions (10 rounds):
  java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1 -v -t 1 -r 10
