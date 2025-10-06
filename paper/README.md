# AST Convergence Paper

This directory contains the academic paper "AST Convergence: A Novel Pattern for Multi-Modal DSL Implementation" by Daniel Szmulewicz.

## Files

- `ast_convergence_paper.tex` - Main LaTeX paper
- `references.bib` - Bibliography file
- `README.md` - This file

## Compilation Instructions

To compile the paper, use:

```bash
pdflatex ast_convergence_paper.tex
bibtex ast_convergence_paper
pdflatex ast_convergence_paper.tex
pdflatex ast_convergence_paper.tex
```

## Required LaTeX Packages

Make sure you have the following LaTeX packages installed:

- `IEEEtran` (for conference formatting)
- `graphicx`
- `amsmath`, `amssymb`, `amsfonts`
- `algorithmic`
- `textcomp`
- `xcolor`
- `listings`
- `booktabs`
- `multirow`
- `ulem`
- `hyperref`

## Abstract

This paper introduces *AST Convergence*, a novel architectural pattern for implementing multi-modal Domain Specific Languages (DSLs) that elegantly solves the classic code duplication problem in language implementation. AST Convergence enables both external (text-based) and internal (code-based) DSL modalities to converge on identical Abstract Syntax Tree (AST) structures before semantic transformation, allowing a single transformation pipeline to handle both input sources. We present a formal definition of the pattern, demonstrate its implementation in the Bioscoop FFmpeg DSL, and provide quantitative evaluation showing significant reductions in code duplication while guaranteeing behavioral consistency between modalities.

## Keywords

Domain Specific Languages, AST, Multi-modal Processing, Code Generation, Clojure, Language Implementation

## About the Author

Daniel Szmulewicz is an independent researcher based in Paris, France, specializing in programming language theory and DSL implementation.