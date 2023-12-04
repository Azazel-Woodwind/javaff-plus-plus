# JavaFF2.1

My attempt to improve the speed and efficiency the AI planner: [JavaFF](https://nms.kcl.ac.uk/planning/software/javaff.html), an implementation of the [FF](https://fai.cs.uni-saarland.de/hoffmann/ff.html) forward search planning system in Java. This is forked from [JavaFF2.1](https://github.com/dpattiso/javaff), a branch of [JavaFF](https://nms.kcl.ac.uk/planning/software/javaff.html) planner by Andrew Coles, to allow parsing and solving of PDDL 2.1 level 1 problems.

# Usage

The usage of the code is the same as [vanilla JavaFF](https://nms.kcl.ac.uk/planning/software/javaff.html). In other words, run javaff.JavaFF.main() with a domain and problem file. Be sure to add the contents of the /lib directory to the classpath.

# Citation

For citing JavaFF 2.1 specifically, use

```
@phdthesis{phdthesis,
  author       = {David Pattison},
  title        = {A New Heuristic Based Model of Goal Recognition Without Libraries},
  school       = {University of Strathclyde},
  year         = 2015
}
```

For citing JavaFF generally, use Coles' paper from 2008

```
@INPROCEEDINGS{ac2008001,
	author = "A. I. Coles and M. Fox and D. Long and A. J. Smith",
	title = "Teaching Forward-Chaining Planning with {JavaFF}",
	booktitle = "Colloquium on {AI} Education, Twenty-Third {AAAI} Conference on Artificial Intelligence",
	year = "2008",
	month = "July",
}
```
