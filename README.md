# Software-Development-Coursework
VERSIONS 

  JUnit : 4.13.2 (JUnit 4.x) with Hamcrest 1.3 (hamcrest-core-1.3.jar) 

  JDK   : 17.0.18

  

CONTENTS 

  Production code : Card, Deck, Player, CardGame     (.java and .class) 

  Tests           : CardTest, DeckTest, PlayerTest, CardGameTest   (.java and .class) 

  Test support    : TestSupport (helpers), AllTests (suite that runs all four) 

  Everything is in the default package, in this one folder. 

  

DEPENDENCIES (not included - download both jars into this folder) 

  https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar 

  https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar 

  

COMPILE (Windows PowerShell, from this folder) 

  javac -cp ".;junit-4.13.2.jar;hamcrest-core-1.3.jar" *.java 

  

RUN THE WHOLE SUITE 

  java -cp ".;junit-4.13.2.jar;hamcrest-core-1.3.jar" org.junit.runner.JUnitCore AllTests 

  

  Expected result: "OK (30 tests)" in roughly one to a few seconds. 

  To run a single class, replace AllTests with e.g. PlayerTest. 

  On macOS/Linux use ':' instead of ';' in the classpath. 

  

PLAY THE GAME ITSELF (not part of the tests) 

  java CardGame 

  then enter the number of players and the path of a pack file. 

  

NOTES 

  - Game tests write playerX_output.txt and deckX_output.txt into the working 

    directory. The tests delete these files before and after every test, so run 

    the suite from a folder where that is acceptable. 

  - Several tests use real threads (deck producers/consumers, a ring of four 

    players, and complete games). They have timeouts, so a deadlock shows up 

    as a failure rather than a hang. 

  - Shuffled packs use fixed seeds, so any failure can be reproduced. 