# STSAnnotations

Java Annotations and Annotation Processor to auto generate common code when adding things using [BaseMod](https://github.com/daviscook477/BaseMod)

## Updating pom.xml
Along with adding this as a dependency you need to add this into your plugins section so the annotation processor is used.
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessors>
            <annotationProcessor>
                kobting.stsannotations.STSAnnotationProcessor
            </annotationProcessor>
        </annotationProcessors>
    </configuration>
</plugin>
```

## @STSAnnotationConfig
Annotation to set the base package that all generated code will go into. This is to avoid possible conflicts with other mods. This can be put on any class but would recommend putting it on your `@SpireInitializer` class.

Example:
```java
@STSAnnotationConfig(basePackage = "kobting.example.generated")
public class Initializer {}
```

## @Card
Annotation to make sure that your card is added to BaseMod in the generated code.

Notes:
 - Card must at some point extend AbstractCard from SlayTheSpire
 - Card must have a no args constructor
 
Example:
Note that this extends CustomCard which extends AbstractCard so it is valid to annotate with @Card
```java
@Card
public class ExampleCard extends CustomCard {
  /*...*/
}
```

## Using generated code
If there are any @Card annotations then `BaseSpireInitializer.java` will be created. In order for the generated code to be used you must have your class using `@SpireInitializer` extend `BaseSpireInitializer`.
`BaseSpireInitializer` lives in the root of the package specified in the `@STSAnnotationConfig`

Example:
```java
import kobting.example.generated.BaseSpireInitializer;

@SpireInitializer
@STSAnnotationConfig(basePackage = "kobting.example.generated")
public class MyInitializer extends BaseSpireInitializer {
    /* normal @SpireInitializer setup and requirements */
}

```

## Example Generated Code
### BaseSpireInitializer
```java
public class BaseSpireInitializer {
  public BaseSpireInitializer() {
    new kobting.example.generated.cards.CardHandler();
  }
}
```
### CardHandler
You should never have to access CardHandler directly
```java
public final class CardHandler implements EditCardsSubscriber {
  public CardHandler() {
    basemod.BaseMod.subscribe(this);
  }

  @Override
  public void receiveEditCards() {
    basemod.BaseMod.addCard(new kobting.example.cards.ExampleCard());
  }
}
```


