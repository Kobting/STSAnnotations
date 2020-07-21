package kobting.stsannotations.annotationhandlers;

import basemod.interfaces.EditCardsSubscriber;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

public class CardAnnotationHandler {

    public static final String CARD_PACKAGE = "cards";
    public static final String CARD_HANDLER_CLASS_NAME = "CardHandler";

    private Messager messager;
    private Filer filer;
    private Elements elements;
    private Types types;

    public CardAnnotationHandler(Messager messager, Filer filer, Elements elements, Types types) {
        this.messager = messager;
        this.filer = filer;
        this.elements = elements;
        this.types = types;
    }

    public boolean processElements(Set<? extends Element> cardAnnotationElements, String basePackage) throws IOException {

        ArrayList<Element> cardInfo = new ArrayList<>();

        for(Element cardAnnotationElement: cardAnnotationElements) {
            if(cardAnnotationElement.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Card Annotation can only be applied to a Class");
            }
            if(!hasNoArgsConstructor(cardAnnotationElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Card Annotation can only be applied to a Class with no args constructor");
            }
            if(!extendsAbstractCard(cardAnnotationElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Card Annotation can only be applied to a Class that extends AbstractCard");
            }
            TypeElement element = (TypeElement) cardAnnotationElement;

            cardInfo.add(element);

        }

        if(cardInfo.size() > 0) {
            generateCardAddingFile(cardInfo, basePackage);
            return true;
        } else {
            return false;
        }

    }

    private boolean hasNoArgsConstructor(Element element) {

        for(Element subElement: element.getEnclosedElements()) {
            if(subElement.getKind() == ElementKind.CONSTRUCTOR && subElement.getModifiers().contains(Modifier.PUBLIC)) {
                TypeMirror typeMirror = subElement.asType();
                if(typeMirror.accept(noArgsVisitor, null)) return true;
            }
        }

        return false;
    }

    private boolean extendsAbstractCard(Element element) {
        for(TypeMirror typeMirror : types.directSupertypes(element.asType())) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            if(declaredType.asElement().getSimpleName().toString().equals("AbstractCard")) return true;
            return extendsAbstractCard(((DeclaredType) typeMirror).asElement());
        }
        return false;
    }

    private final TypeVisitor<Boolean, Void> noArgsVisitor = new SimpleTypeVisitor8<Boolean, Void>() {
        @Override
        public Boolean visitExecutable(ExecutableType t, Void aVoid) {
            return t.getParameterTypes().isEmpty();
        }
    };


    private void generateCardAddingFile(ArrayList<? extends Element> cardInfo, String basePackage) throws IOException {
        TypeSpec.Builder cardHandlerClass = TypeSpec.classBuilder(CARD_HANDLER_CLASS_NAME).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        cardHandlerClass.addSuperinterface(EditCardsSubscriber.class);

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder();
        constructor.addModifiers(Modifier.PUBLIC);
        constructor.addStatement("basemod.BaseMod.subscribe(this)");
        cardHandlerClass.addMethod(constructor.build());

        MethodSpec.Builder methodReceiveEditCards = MethodSpec.methodBuilder("receiveEditCards");
        methodReceiveEditCards.addAnnotation(Override.class);
        methodReceiveEditCards.addModifiers(Modifier.PUBLIC);

        cardInfo.forEach(element -> {
            methodReceiveEditCards.addStatement("basemod.BaseMod.addCard(new $L())", element.asType());
            messager.printMessage(Diagnostic.Kind.NOTE, "Found card: " + element.getSimpleName());
        });

        cardHandlerClass.addMethod(methodReceiveEditCards.build());

        JavaFile.builder(basePackage + "." + CARD_PACKAGE, cardHandlerClass.build()).build().writeTo(filer);

    }

}
