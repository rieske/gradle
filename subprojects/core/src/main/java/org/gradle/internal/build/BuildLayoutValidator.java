/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.build;

import org.gradle.api.GradleException;
import org.gradle.api.internal.DocumentationRegistry;
import org.gradle.api.internal.StartParameterInternal;
import org.gradle.configuration.project.BuiltInCommand;
import org.gradle.initialization.BuildClientMetaData;
import org.gradle.initialization.layout.BuildLayout;
import org.gradle.initialization.layout.BuildLayoutConfiguration;
import org.gradle.initialization.layout.BuildLayoutFactory;
import org.gradle.internal.exceptions.FailureResolutionAware;
import org.gradle.internal.scripts.ScriptFileResolver;
import org.gradle.internal.service.scopes.Scopes;
import org.gradle.internal.service.scopes.ServiceScope;

import java.util.List;

import static org.gradle.internal.logging.text.StyledTextOutput.Style.UserInput;

@ServiceScope(Scopes.BuildSession.class)
public class BuildLayoutValidator {
    private final BuildLayoutFactory buildLayoutFactory;
    private final ScriptFileResolver scriptFileResolver;
    private final DocumentationRegistry documentationRegistry;
    private final BuildClientMetaData clientMetaData;
    private final List<BuiltInCommand> builtInCommands;

    public BuildLayoutValidator(
        BuildLayoutFactory buildLayoutFactory,
        ScriptFileResolver scriptFileResolver,
        DocumentationRegistry documentationRegistry,
        BuildClientMetaData clientMetaData,
        List<BuiltInCommand> builtInCommands
    ) {
        this.buildLayoutFactory = buildLayoutFactory;
        this.scriptFileResolver = scriptFileResolver;
        this.documentationRegistry = documentationRegistry;
        this.clientMetaData = clientMetaData;
        this.builtInCommands = builtInCommands;
    }

    public void validate(StartParameterInternal startParameter) {
        BuildLayout buildLayout = buildLayoutFactory.getLayoutFor(new BuildLayoutConfiguration(startParameter));
        if (!buildLayout.isBuildDefinitionMissing()) {
            // All good
            return;
        }

        for (BuiltInCommand command : builtInCommands) {
            if (command.commandLineMatches(startParameter.getTaskNames())) {
                // Allow missing settings and build scripts when running a built-in command
                return;
            }
        }

        StringBuilder message = new StringBuilder();
        message.append("Directory '");
        message.append(startParameter.getCurrentDir());
        message.append("' does not contain a Gradle build.\n\n");
        message.append("A Gradle build should contain a 'settings.gradle' or 'settings.gradle.kts' file in its root directory. ");
        message.append("It may also contain a 'build.gradle' or 'build.gradle.kts' file.\n\n");
        message.append("To create a new Gradle build in this directory run '");
        clientMetaData.describeCommand(message, "init");
        message.append("'\n\n");
        message.append("For more detail on the 'init' task see ");
        message.append(documentationRegistry.getDocumentationFor("build_init_plugin"));
        message.append("\n\n");
        message.append("For more detail on creating a Gradle build see ");
        message.append(documentationRegistry.getDocumentationFor("tutorial_using_tasks")); // this is the "build script basics" chapter, we're missing some kind of "how to write a Gradle build chapter"
        throw new BuildLayoutException(message.toString());
    }

    private static class BuildLayoutException extends GradleException implements FailureResolutionAware {
        public BuildLayoutException(String message) {
            super(message);
        }

        @Override
        public void appendResolutions(Context context) {
            context.doNotSuggestResolutionsThatRequireBuildDefinition();
            context.appendResolution(output -> {
                output.text("Run ");
                context.getClientMetaData().describeCommand(output.withStyle(UserInput), "init");
                output.text(" to create a new Gradle build in this directory.");
            });
        }
    }
}
