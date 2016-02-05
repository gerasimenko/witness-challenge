# Computer vision assistant for The Witness challenge

This program solve in real time color puzzles(3-monitor) and triangle puzzles(maze) from the Witness challenge.

#How to use
I tried to write it as simple as possible, so there is no dependencies and libraries, just Java standard library classes.
Program has two parameters: your screenshot folder, where Steam puts screenshots for Witness, and output folder, where you will see solved puzzles.
When running, it checks every second for new screenshot, if found, tries to find puzzle on it and solve.

Build command:
    javac Solver.java

Run command:
    java -Xss64m -cp . Solver "STEAM\SCREENSHOT\DIRECTORY\" "OUTPUT\DIRECTORY\"

Program has infinity cycle inside, so just stop it manually, when you finish.

When you make a screenshot, try to align your view directly to the puzzle, not view from top, let's puzzle form will be close to a square.

#How it works
[Examples of solved puzzles](http://imgur.com/a/FEBQG)

For puzzle detection I used DFS and simple statistics calculations, for puzzle solving I used backtracking with DFS.