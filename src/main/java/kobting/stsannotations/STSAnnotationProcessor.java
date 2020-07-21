package kobting.stsannotations;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import kobting.stsannotations.annotationhandlers.CardAnnotationHandler;
import kobting.stsannotations.annotations.Card;
import kobting.stsannotations.annotations.STSAnnotationConfig;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

@SupportedAnnotationTypes({"kobting.stsannotations.annotations.Card", "kobting.stsannotations.annotations.STSAnnotationConfig"})
@AutoService(Processor.class)
public class STSAnnotationProcessor extends AbstractProcessor {

    private Messager messager;
    private Filer filer;
    private Elements elements;
    private Types types;
    private CardAnnotationHandler cardAnnotationHandler;

    private String basePackage;

    private boolean generatedInitializer = false;
    private boolean foundStsAnnotation = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        cardAnnotationHandler = new CardAnnotationHandler(messager, filer, elements, types);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if(!roundEnv.processingOver()) {
            try {
                handleSTSAnnotationConfig(roundEnv.getElementsAnnotatedWith(STSAnnotationConfig.class));
                boolean succeeded = cardAnnotationHandler.processElements(roundEnv.getElementsAnnotatedWith(Card.class), basePackage);
                generateSpireInitializer(succeeded, basePackage);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        if(!foundStsAnnotation) {
            messager.printMessage(Diagnostic.Kind.ERROR, "At least 1 class must be annotated with STSAnnotationConfig");
        }

        return true;
    }

    private void handleSTSAnnotationConfig(Set<? extends Element> elements) {
        if(elements.size() > 0) {
            Element configuration = elements.iterator().next();
            for (AnnotationMirror annotationMirror : configuration.getAnnotationMirrors()) {
                if(annotationMirror.getAnnotationType().asElement().getSimpleName().toString().equals("STSAnnotationConfig")) {
                    for (ExecutableElement executableElement : annotationMirror.getElementValues().keySet()) {
                        foundStsAnnotation = true;
                        if(executableElement.getSimpleName().toString().equals("basePackage")) {
                            basePackage = (String) annotationMirror.getElementValues().get(executableElement).getValue();
                        } else {
                            messager.printMessage(Diagnostic.Kind.ERROR, "Something failed. " + executableElement + " Value: " + annotationMirror.getElementValues().get(executableElement));
                        }
                    }
                }
            }
        }
    }

    private void generateSpireInitializer(boolean cardAnnotationsSucceeded, String basePackage) throws IOException {
        if(!generatedInitializer && cardAnnotationsSucceeded) {
            TypeSpec.Builder initializerClass = TypeSpec.classBuilder("BaseSpireInitializer").addModifiers(Modifier.PUBLIC);

            MethodSpec.Builder constructor = MethodSpec.constructorBuilder();
            constructor.addModifiers(Modifier.PUBLIC);
            constructor.addStatement("new " + basePackage + "." + CardAnnotationHandler.CARD_PACKAGE + "." + CardAnnotationHandler.CARD_HANDLER_CLASS_NAME + "()");

            initializerClass.addMethod(constructor.build());

            generatedInitializer = true;
            JavaFile.builder(basePackage, initializerClass.build()).build().writeTo(filer);
        }
    }

}
