# wicket-css-scope ![Java CI](https://github.com/42Lines/wicket-css-scope/workflows/Java%20CI/badge.svg?branch=master)

Maven plugin that compiles and applies automatic namespacing to css so that it is localized and portable with the wicket:panel.


### Now with Sass support

Inside the markup file if the compiler finds a <wicket:scss> </wicket:scss> block it will run the contents through the libscss compiler and include that content in the wicket:head style block.


#### Example

```html
<html>
<wicket:scss>

@import "common.scss";

$my-color:#9249da;

.scssRule {}
#idSelector {}
</wicket:scss>

<wicket:head>
  <style>
    .normalCssRule {}
  </style>
</wicket:head>

<wicket:panel>
   <div>
      <section class="scssRule additionalSelector">
        <span class="normalCssRule">Test</span>
        <span id="idSelector">Id Selected</span>
      </section>
   </div>
</wicket:panel>
</html>
```

Transforms to
```html
<html>
<wicket:head>
  <style>
    .abcde_fghij {}
    .abcde_klmno {}
    .abcde_pqrst {}
  </style>
</wicket:head>

<wicket:panel>
   <div class="abcde">
      <section class="abcde_klmno additionalSelector">
        <span class="abcde_fghij">Test</span>
        <span class="abcde_pqrst">Id Selected</span>
      </section>
   </div>
</wicket:panel>
</html>
```

where 'abcde' is the local scope designator for the wicket panel.  Additionally the ID selectors will be rewritten as class selectors, since there is no guarantee of uniqueness of the id if the wicket:panel is included more than one time in the page.
Also you can see that because the 'additionalSelector' class is not referenced by any local style rules, it is left intact in the resulting panel.  This allows for mixing of globally defined style rules with localized rules.

### Special Annotations

#### The *@external* annotation

Using the @external annotation bypasses the scoping and emits the style into the global scope.

```css
@external .globalCssRule {}
```

Becomes

```css
.globalCssRule {}
```

#### The *@container* annotation
Using the @container annotation prefixes the rule with the local scope, but doesn't alter any of the child selectors.  This can be used when an outer class needs to control the style for an inner class that is defined in a different file.

```css
@container .containerCssRule {}
```
Becomes
```css
.abcde .containerCssRule {}
```


