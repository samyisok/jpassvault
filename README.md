[![Build(maven)](https://github.com/samyisok/jpassvault/actions/workflows/maven.yml/badge.svg)](https://github.com/samyisok/jpassvault/actions/workflows/maven.yml)

# Jpassvault

Password vault on JavaFx and Spring boot with online sync.

In the modern world, we have all things that use bazillion external libs and overcomplicated functional.
Of course, it isn't easy to verify external lib for security vulnerabilities. To solve this, we can use a minimal amount of external libs and simplified features. It would be easy to check code for yourself if the codebase is small and features are less.

# Interface

Usually, we need only save a new generated password to the vault and copy from the vault, and that is the goal for this interface. We can add and copy with a few clicks, and we do not need to save additional information. Here implemented 14 char length password generation with upper, lower chars, digits, and few special chars, because that is secure and the most common and average requirement for most websites.

![Example interface](https://raw.githubusercontent.com/samyisok/jpassvault/main/doc/readme-asset.gif)

# Requirements

Any machine that can launch JavaVM and have GUI. It should work on Mac, Windows, and Linux.

# Online sync functional

You can choose in setting where you can save the original vault DB, including cloud disk storage. Also, you can use a compatible sync server like [jpassvaultserver](https://github.com/samyisok/jpassvaultserver).

Db vault is secured with "AES/GCM/NoPadding" that should be most secured according to [sonarsource](https://rules.sonarsource.com/java/RSPEC-4432) and [Aritcles from the internet](https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9).
And will use another separate masterkey when vault first created.

# Installation

Installation proceeds with according principle *BYDY* - Build Yourself, Deploy Yourself.

# Settings

Folder for backup vault and db placed in app folder of user's home folder.
Backup Db will be saved each time when app make merges db from online sync.

