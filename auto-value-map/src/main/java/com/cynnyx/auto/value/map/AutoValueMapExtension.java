/*
 * Copyright (c) 2016 Cynny SpA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cynnyx.auto.value.map;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;

@AutoService(AutoValueExtension.class)
public class AutoValueMapExtension extends AutoValueExtension {

    @Override public boolean applicable(Context context) {
        return MapMethod.filteredAbstractMethods(context).size() > 0;
    }

    @Override public Set<ExecutableElement> consumeMethods(Context context) {
        return MapMethod.filteredAbstractMethods(context);
    }

    @Override public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
        List<Property> properties = Property.readProperties(context.properties());

        Set<String> complexProperties = new HashSet<>();
        for (ExecutableElement method : ElementFilter.methodsIn(context.autoValueClass().getEnclosedElements())) {
            TypeMirror returnType = method.getReturnType();
            TypeName typeName = TypeName.get(returnType);
            if (typeName.isPrimitive() || typeName.isBoxedPrimitive()) {
                // skip primitives
                continue;
            }
            TypeElement element = context.processingEnvironment().getElementUtils().getTypeElement(returnType.toString());
            if (element != null) {
                if (ElementKind.CLASS.equals(element.getKind())) {
                    Name qualifiedName = element.getQualifiedName();
                    if (qualifiedName.contentEquals("java.lang.String")) {
                        continue;
                    }
                    // complex type need to be toMap()-ped
                    complexProperties.add(method.getSimpleName().toString());
                }
            }
        }

        TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
                .superclass(TypeVariableName.get(classToExtend))
                .addMethod(ConstructorMethod.generateConstructor(properties))
                .addMethod(MapMethod.generateMethod(properties, complexProperties));

        if (isFinal) {
            subclass.addModifiers(FINAL);
        } else {
            subclass.addModifiers(ABSTRACT);
        }

        return JavaFile.builder(context.packageName(), subclass.build()).build().toString();
    }
}
