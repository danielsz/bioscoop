(ns bioscoop.domain.specs.shared.color
  (:require [clojure.spec.alpha :as s])
  (:import [java.lang IllegalStateException]))

(s/def ::named-color
  #{"aliceblue" "antiquewhite" "aqua" "aquamarine" "azure" "beige" "bisque" "black"
    "blanchedalmond" "blue" "blueviolet" "brown" "burlywood" "cadetblue" "chartreuse"
    "chocolate" "coral" "cornflowerblue" "cornsilk" "crimson" "cyan" "darkblue" "darkcyan"
    "darkgoldenrod" "darkgray" "darkgreen" "darkgrey" "darkkhaki" "darkmagenta" "darkolivegreen"
    "darkorange" "darkorchid" "darkred" "darksalmon" "darkseagreen" "darkslateblue" "darkslategray"
    "darkslategrey" "darkturquoise" "darkviolet" "deeppink" "deepskyblue" "dimgray" "dimgrey"
    "dodgerblue" "firebrick" "floralwhite" "forestgreen" "fuchsia" "gainsboro" "ghostwhite" "gold"
    "goldenrod" "gray" "green" "greenyellow" "grey" "honeydew" "hotpink" "indianred" "indigo" "ivory"
    "khaki" "lavender" "lavenderblush" "lawngreen" "lemonchiffon" "lightblue" "lightcoral" "lightcyan"
    "lightgoldenrodyellow" "lightgray" "lightgreen" "lightgrey" "lightpink" "lightsalmon" "lightseagreen"
    "lightskyblue" "lightslategray" "lightslategrey" "lightsteelblue" "lightyellow" "lime" "limegreen"
    "linen" "magenta" "maroon" "mediumaquamarine" "mediumblue" "mediumorchid" "mediumpurple"
    "mediumseagreen" "mediumslateblue" "mediumspringgreen" "mediumturquoise" "mediumvioletred"
    "midnightblue" "mintcream" "mistyrose" "moccasin" "navajowhite" "navy" "oldlace" "olive" "olivedrab"
    "orange" "orangered" "orchid" "palegoldenrod" "palegreen" "paleturquoise" "palevioletred" "papayawhip"
    "peachpuff" "peru" "pink" "plum" "powderblue" "purple" "red" "rosybrown" "royalblue" "saddlebrown"
    "salmon" "sandybrown" "seagreen" "seashell" "sienna" "silver" "skyblue" "slateblue" "slategray"
    "slategrey" "snow" "springgreen" "steelblue" "tan" "teal" "thistle" "tomato" "turquoise" "violet"
    "wheat" "white" "whitesmoke" "yellow" "yellowgreen" "random"}) ; FFmpeg known names + 'random'


(defn capture [s]
  (let [patt (re-pattern "^(?:(?<name>[a-zA-Z]+)|(?<hex>(?:0x|#)[0-9a-fA-F]{6}))(?:@(?<opacity>\\d*\\.?\\d+))?|(?<hexalpha>(?:0x|#)[0-9a-fA-F]{8})$")
        matcher (re-matcher patt s)]
    (re-find matcher)
    (try (if-let [colorname (.group matcher "name")]
           (s/valid? ::named-color colorname)
           true)
         (catch IllegalStateException _))))

(s/def ::color capture)


