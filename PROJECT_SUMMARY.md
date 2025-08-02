
# Table of Contents

1.  [Bioscoop: FFmpeg Filtergraph DSL](#org73cebaa)
    1.  [Core Architecture](#org3fd66e7)
        1.  [1. **DSL Parser (`src/bioscoop/dsl.clj`)**](#org808901f)
        2.  [2. **Domain Model (`src/bioscoop/domain/filtergraph.clj`)**](#org566d55b)
        3.  [3. **Core Data Structures**](#org4122d71)
        4.  [4. **Rendering Engine (`src/bioscoop/render.clj`)**](#org532d656)
2.  [5. *Bidirectional Translation*](#org1c7c822)
    1.  [Produces valid FFmpeg filtergraph strings](#org4417675)
        1.  [**FFmpeg Parser (`src/bioscoop/ffmpeg_parser.clj`)**](#org397b74e)
    2.  [DSL Features](#org7397df2)
        1.  [Basic Filter Creation](#org000e854)
        2.  [Filter Chains](#org06f3b36)
        3.  [Let Bindings for Variables](#orgc7dc26a)
        4.  [Input/Output Labels](#org72ec0e5)
        5.  [Built-in Filter Functions](#orga632ac8)
    3.  [Key Dependencies](#org5ea1024)
    4.  [Usage Flow](#org4bd5ab2)
    5.  [Example End-to-End](#orgf670d0f)


<a id="org73cebaa"></a>

# Bioscoop: FFmpeg Filtergraph DSL

**Bioscoop** is a Clojure library that provides a domain-specific language (DSL) for building FFmpeg filtergraphs. It allows you to write video/audio filter operations in a Lisp-like syntax that compiles down to FFmpeg's native filtergraph format.


<a id="org3fd66e7"></a>

## Core Architecture

The project is structured around several key components:


<a id="org808901f"></a>

### 1. **DSL Parser (`src/bioscoop/dsl.clj`)**

-   Uses **Instaparse** to parse a Lisp-like syntax defined in `resources/lisp-grammar.bnf`
-   Supports typical Lisp constructs: functions, let bindings, symbols, keywords, strings, numbers, booleans
-   Grammar allows for expressions like `(scale 1920 1080)` and `(let [width 1920] (scale width 1080))`


<a id="org566d55b"></a>

### 2. **Domain Model (`src/bioscoop/domain/filtergraph.clj`)**

-   Uses Clojure Spec to define the structure of filters, filter chains, and filtergraphs
-   Provides validation for the core data structures


<a id="org4122d71"></a>

### 3. **Core Data Structures**

Three main records represent FFmpeg concepts:

-   **`Filter`**: A single filter operation (e.g., scale, overlay, crop)
-   **`FilterChain`**: A sequence of filters connected in series (comma-separated in FFmpeg)
-   **`FilterGraph`**: Multiple filter chains running in parallel (semicolon-separated in FFmpeg)


<a id="org532d656"></a>

### 4. **Rendering Engine (`src/bioscoop/render.clj`)**

-   Implements the `FFmpegRenderable` protocol to convert data structures to FFmpeg syntax
-   Handles input/output labels for complex filtergraphs
-   Ensures proper escaping and formatting


<a id="org1c7c822"></a>

# 5. *Bidirectional Translation*

-   /=src/bioscoop/render.clj=\*: Converts DSL → FFmpeg filtergraph strings
-   /=src/bioscoop/ffmpeg.clj=\*: Parses existing FFmpeg filtergraph strings → DSL structures

\*/ Example Usage

From the tests, here's how the DSL works:

    ;; Simple filter: scale video to 1920x1080
    (scale 1920 1080)
    ;; Renders to: "scale=1920:1080"
    
    ;; With let bindings for reusable values
    (let [width 1920 height 1080]
      (scale width height))
    
    ;; Complex filtergraph with multiple chains
    [(scale 1920 1080) 
     (overlay 10 10)]
    ;; Creates parallel filter chains
    
    ;; Function definitions for reusable components
    (def standard-scale (fn [w h] (scale w h)))

\*/ Key Features

1.  *Type Safety*: Uses Clojure Spec for validation
2.  *Composability*: Filters can be combined into chains and graphs
3.  *Reusability*: Support for function definitions and let bindings
4.  *Bidirectional*: Can both generate and parse FFmpeg filtergraphs
5.  *Extensible*: Easy to add new filter types

\*/ Data-First Design

Following Clojure's philosophy, the library prioritizes data structures:

-   Filters are represented as simple maps with `:name` and `:arguments`
-   Filter chains are vectors of filters
-   Filter graphs are vectors of filter chains
-   The DSL provides a more ergonomic syntax that compiles to these structures

\*/ Use Cases

This DSL would be particularly useful for:

-   Video processing pipelines where filtergraphs need to be generated programmatically
-   Template-based video generation
-   Complex filter operations that benefit from Clojure's functional composition
-   Applications that need to both generate and parse FFmpeg filter strings

The project demonstrates excellent Clojure design principles: immutable data structures, clear separation of parsing/rendering concerns, comprehensive testing, and a data-first approach to domain modeling.


<a id="org4417675"></a>

## Produces valid FFmpeg filtergraph strings


<a id="org397b74e"></a>

### **FFmpeg Parser (`src/bioscoop/ffmpeg_parser.clj`)**

-   Can parse existing FFmpeg filtergraph strings back into the internal data structures
-   Provides bidirectional conversion (DSL ↔ FFmpeg string)


<a id="org7397df2"></a>

## DSL Features


<a id="org000e854"></a>

### Basic Filter Creation

    ;; Named function approach
    (scale 1920 1080)          ; → "scale=1920:1080"
    
    ;; Generic filter approach  
    (filter "scale" "1920:1080") ; → "scale=1920:1080"


<a id="org06f3b36"></a>

### Filter Chains

    (chain (scale 1920 1080) (overlay)) ; → "scale=1920:1080,overlay"


<a id="orgc7dc26a"></a>

### Let Bindings for Variables

    (let [width 1920 
          height 1080] 
      (scale width height))     ; → "scale=1920:1080"


<a id="org72ec0e5"></a>

### Input/Output Labels

    (let [input-vid (input-labels "in")
          scaled (filter "scale" "1920:1080" input-vid (output-labels "scaled"))]
      scaled)                   ; → "[in]scale=1920:1080[scaled]"


<a id="orga632ac8"></a>

### Built-in Filter Functions

The DSL includes predefined functions for common filters:

-   `scale`, `crop`, `overlay`, `fade`
-   Arithmetic operators: `+`, `-`, `*`, `/`
-   Utility functions: `str`


<a id="org5ea1024"></a>

## Key Dependencies

-   **Instaparse**: For parsing the DSL grammar
-   **FFmpeg Java wrapper**: For integration with FFmpeg
-   **Clojure Spec**: For data validation
-   **Logback**: For logging


<a id="org4bd5ab2"></a>

## Usage Flow

1.  **Write DSL code** in the Lisp-like syntax
2.  **Parse** using the grammar defined in `lisp-grammar.bnf`
3.  **Transform** the AST into Filter/FilterChain/FilterGraph records
4.  **Render** to FFmpeg filtergraph strings
5.  **Execute** with FFmpeg


<a id="orgf670d0f"></a>

## Example End-to-End

    ;; DSL Input
    "(chain (scale 1920 1080) (overlay))"
    
    ;; Parsed & Compiled to
    FilterGraph{
      :chains [FilterChain{
        :filters [Filter{:name "scale", :args "1920:1080"}
                  Filter{:name "overlay", :args nil}]}]}
    
    ;; Rendered to FFmpeg
    "scale=1920:1080,overlay"

This architecture provides a clean separation between the high-level DSL, the internal representation, and the low-level FFmpeg syntax, making it easier to build complex video processing pipelines programmatically.

