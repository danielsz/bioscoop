# Bioscoop Paper

This directory contains the paper *Bioscoop: A Domain-Specific Language for FFmpeg Filtergraph Composition* by Daniel Szmulewicz, prepared for *The Art, Science, and Engineering of Programming* (submission mode).

## Files

- `programming/bioscoop_paper.tex` — main LaTeX paper
- `programming/bioscoop.bib` — bibliography used by the paper
- `programming/programming.cls` — journal class file
- `programming/screenshot.png` — figure used in the paper
- `README.md` — this file

The top-level `references.bib` belongs to an earlier draft and is not used by the current paper.

## Compilation Instructions

The paper uses `biblatex` with `biber` and the journal's `programming` class. From this directory:

```bash
cd programming
pdflatex bioscoop_paper.tex
biber bioscoop_paper
pdflatex bioscoop_paper.tex
pdflatex bioscoop_paper.tex
```

If your `latexmk` is configured for `biber`, `latexmk -pdf bioscoop_paper.tex` from the `programming` directory should also work.

## Required LaTeX Packages

- The `programming` class (included in `programming/`)
- `biblatex` and `biber`
- `csquotes`
- `amsthm`
- A standard TeX Live installation with the packages loaded by `programming.cls` (including `listings`, `booktabs`, `graphicx`, `hyperref`, and `xcolor`)

## Summary

Bioscoop is a domain-specific language compiler for FFmpeg filtergraph composition. It provides two front-ends — an external text-based DSL parsed with a GLL parser and an internal macro-based DSL embedded in Clojure — that converge on a shared AST before evaluation. The paper describes the architecture, the isomorphic intermediate representation, first-class iteration, two-phase locals injection via `&env`, fail-soft error handling, and dual JVM/GraalVM resolution strategies, and presents a slideshow generator case study.

## Keywords

Domain-specific languages, FFmpeg, filtergraph, multi-modal DSL, Clojure, bicameral parsing, intermediate representation, two-phase locals injection
