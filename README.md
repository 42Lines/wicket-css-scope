# wicket-css-scope ![Java CI](https://github.com/42Lines/wicket-css-scope/workflows/Java%20CI/badge.svg?branch=master)

Maven plugin that compiles and applies automatic namespacing to css so that it is localized and portable with the wicket:panel.


### Now with Sass support

Inside the markup file if the compiler finds a <wicket:scss> </wicket:scss> block it will run the contents through the libscss compiler and include that content in the wicket:head style block.

```css
<html>
<wicket:scss>

@import "common.scss";

$my-color:#9249da;

.scssRule {}

</wicket:scss>

<wicket:head>
  <style>
    .normalCssRule {}
  </style>
</wicket:head>

<wicket:panel>
   <div>
      <section class="scssRule">
        <span class="normalCssRule">Test</span>
      </section>
   </div>
</wicket:panel>
</html>
```
