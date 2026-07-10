IntelliJ Plugin for gvc0
========================

This is the IntelliJ plugin for the gradual verifier for the C0 subset of C.

Usage
-----

Tools > Symbolic Execution

Installation on Linux/macOS
---------------------------

1. Install the following

- Eclipse Adoptium JDK 17

- Scala >= 2.12.18 (Coursier is recommended to install Scala)

- IntelliJ IDEA Ultimate or Community Edition 2022.3.3 with the Scala plugin

2. Set up `gvc0`

Clone the following repositories into the same parent directory.

```
git clone https://github.com/gradual-verification/silver-gv.git
git clone https://github.com/gradual-verification/silicon-gv.git
git clone https://github.com/gradual-verification/gvc0.git
git clone https://github.com/advancingdragon/c0.git
```

3. Install [Z3](https://github.com/Z3Prover/z3/releases) and
[cc0](https://bitbucket.org/c0-lang/docs/wiki/Downloads). For macOS runnning
on Apple Silicon, install `cc0` from
[https://nguyen.bz/compiler-c0.zip](https://nguyen.bz/compiler-c0.zip).

Directories where Z3 and `cc0` binaries are located should be in `PATH`.
`gmp` and `gnu-getopt` are also needed for the `cc0` compiler to work.

4. Build `gvc0`

```
cd ./silicon-gv
ln -s ../silver-gv silver
cd ../gvc0
ln -s ../silicon-gv silicon
sbt assembly
```

5. Set environment variable `GVC0_PATH` to `gvc0` directory.

6. Open project `c0` in IntelliJ IDEA. Go to menu Run > Debug 'Run Plugin'.

Installation on Windows (WSL not required)
------------------------------------------

1. Install the following

- [Oracle JDK 17](https://www.oracle.com/java/technologies/downloads/)

- IntelliJ IDEA Ultimate or Community Edition 2022.3.3 with the Scala plugin

Installing Scala is not required as IntelliJ IDEA will automatically install
Scala and sbt the first time a Scala project is opened.

2. Set up `gvc0`

Clone the following repositories into the same parent directory.

```
git clone https://github.com/gradual-verification/silver-gv.git
git clone https://github.com/gradual-verification/silicon-gv.git
git clone https://github.com/gradual-verification/gvc0.git
git clone https://github.com/advancingdragon/c0.git
```

Important: Add the aforementioned parent directory into the Windows Defender
Exclusion list

Windows Security > Virus & threat protection > Manage settings >
Add or remove exclusions > Add an exclusion

3. Install [Z3](https://github.com/Z3Prover/z3/releases). Directory where Z3
exe file is located should be in the `Path` environment variable.

4. Build `gvc0`

```
cd silicon-gv
mklink /d silver ..\silver-gv
cd ..\gvc0
mklink /d silicon ..\silicon-gv
```

Open project `gvc0` in IntelliJ IDEA. Open the sbt shell. Then run

```
set assembly / test := {}
assembly
```

5. Set environment variable `GVC0_PATH` to `gvc0` directory.

6. Open project `c0` in IntelliJ IDEA. Go to menu Run > Debug 'Run Plugin'.

Limitations: cannot run `cc0` yet, because it requires WSL.
