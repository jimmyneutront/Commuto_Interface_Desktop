# Commuto Interface: Desktop

The Commuto Protocol is a decentralized exchange that allows users to exchange fiat currencies and stablecoins quickly,
affordably, safely, and in a censorship-resistant manner, without the use of a trusted intermediary.

The official Solidity implementation of the Commuto Protocol can be found
[here](https://github.com/jimmyneutront/commuto-protocol), and more information about Commuto can be found
[here](https://jimmyneutront.github.io/commuto-docs/).

## About this Repository

This repository contains the Commuto Desktop Interface, a Java application for macOS, Windows and Linux that allows for easy interaction with the Commuto Protocol and Commuto's peer-to-peer network, which is built on top of [Matrix](https://matrix.org).

## Testing/Developing the Commuto Desktop Interface

1. Ensure you have set up a working Commuto Protocol development environment.

2. Ensure that you have JDK 16 installed:

   ```
   $ javac -version
   ```

3. Install [IntelliJ IDEA](https://www.jetbrains.com/idea/)

4. Open IntelliJ IDEA and select `File` > `New` > `Project from Version Control...`

5. Clone from this repository url: [`https://github.com/jimmyneutront/Commuto_Interface_Desktop.git`](https://github.com/jimmyneutront/Commuto_Interface_Desktop.git)

6. Trust the project, and wait for IntelliJ to finish importing and building the project.

7. Navigate to your `npm` project within your commuto-protocol repository.

8. Start a standalone Hardhat Network instance:

   ```
   $ npx hardhat node
   ```

9. Set up an on-chain testing environment by running [Setup_Test_Environment.py](https://github.com/jimmyneutront/commuto-protocol/blob/f29c18e0757c4f79ce9335b8ec863d7de762ffb8/Setup_Test_Environment.py#L14)

10. Return to your Commuto Interface Desktop repo, and replace the web3 provider address in [CommutoCoreInteraction.kt](https://github.com/jimmyneutront/Commuto_Interface_Desktop/blob/master/src/test/kotlin/com/commuto/interfacedesktop/CommutoCoreInteraction.kt) with the address and port number of your Hardhat Network instance.

11. You are now ready to run tests on and develop the Commuto Desktop interface! 