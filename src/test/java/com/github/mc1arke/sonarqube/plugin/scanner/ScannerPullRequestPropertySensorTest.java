package com.github.mc1arke.sonarqube.plugin.scanner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.System2;
import org.sonar.scanner.scan.ProjectConfiguration;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.AzureDevOpsServerPullRequestDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.GitlabServerPullRequestDecorator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScannerPullRequestPropertySensorTest {

    private ScannerPullRequestPropertySensor sensor;

    private final System2 system2 = mock(System2.class);
    private final ProjectConfiguration projectConfiguration = mock(ProjectConfiguration.class);
    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testPropertySensorWithGitlabCIEnvValues() throws IOException {
        
        Path temp = Files.createTempDirectory("sensor");

        DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo").initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n").build();
        SensorContextTester context = SensorContextTester.create(temp);
        context.fileSystem().add(inputFile);        

        when(system2.envVariable("GITLAB_CI")).thenReturn("true");
        when(system2.envVariable("CI_API_V4_URL")).thenReturn("value");
        when(system2.envVariable("CI_PROJECT_PATH")).thenReturn("value");
        when(system2.envVariable("CI_MERGE_REQUEST_PROJECT_URL")).thenReturn("value");
        when(system2.envVariable("CI_PIPELINE_ID")).thenReturn("value");        

        sensor = new ScannerPullRequestPropertySensor(projectConfiguration, system2);
        sensor.execute(context);

        Map<String, String> properties = context.getContextProperties();

        assertEquals(4, properties.size());        
    }    

    @Test
    public void testPropertySensorWithGitlabEnvValues() throws IOException {
        
        Path temp = Files.createTempDirectory("sensor");

        DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo").initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n").build();
        SensorContextTester context = SensorContextTester.create(temp);
        context.fileSystem().add(inputFile);        

        when(system2.envVariable("GITLAB_CI")).thenReturn("false");
        when(system2.property(GitlabServerPullRequestDecorator.PULLREQUEST_GITLAB_INSTANCE_URL)).thenReturn("value");
        when(system2.property(GitlabServerPullRequestDecorator.PULLREQUEST_GITLAB_PROJECT_ID)).thenReturn("value");
        when(system2.property(GitlabServerPullRequestDecorator.PULLREQUEST_GITLAB_PROJECT_URL)).thenReturn("value");
        when(system2.property(GitlabServerPullRequestDecorator.PULLREQUEST_GITLAB_PIPELINE_ID)).thenReturn("value");        

        sensor = new ScannerPullRequestPropertySensor(projectConfiguration, system2);
        sensor.execute(context);

        Map<String, String> properties = context.getContextProperties();

        assertEquals(4, properties.size());        
    }    

    @Test
    public void testPropertySensorWithAzureDevOpsEnvValues() throws IOException {
        
        Path temp = Files.createTempDirectory("sensor");

        DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo").initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n").build();
        SensorContextTester context = SensorContextTester.create(temp);
        context.fileSystem().add(inputFile);        

        when(system2.envVariable("GITLAB_CI")).thenReturn("false");
        when(system2.property(AzureDevOpsServerPullRequestDecorator.AZUREDEVOPS_ENV_INSTANCE_URL)).thenReturn("value");
        when(system2.property(AzureDevOpsServerPullRequestDecorator.AZUREDEVOPS_ENV_TEAMPROJECT_ID)).thenReturn("value");
        when(system2.property(AzureDevOpsServerPullRequestDecorator.AZUREDEVOPS_ENV_REPOSITORY_NAME)).thenReturn("value");
        when(system2.property(AzureDevOpsServerPullRequestDecorator.AZUREDEVOPS_ENV_BASE_BRANCH)).thenReturn("value");    
        when(system2.property(AzureDevOpsServerPullRequestDecorator.AZUREDEVOPS_ENV_BRANCH)).thenReturn("value");    
        when(system2.property(AzureDevOpsServerPullRequestDecorator.AZUREDEVOPS_ENV_PULLREQUEST_ID)).thenReturn("value");    
                
        sensor = new ScannerPullRequestPropertySensor(projectConfiguration, system2);
        sensor.execute(context);

        Map<String, String> properties = context.getContextProperties();

        assertEquals(6, properties.size());        
    }    

    @Test
    public void testPropertySensorWithAzureDevOpsScannerValues() throws IOException {
        
        Path temp = Files.createTempDirectory("sensor");

        DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo").initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n").build();
        SensorContextTester context = SensorContextTester.create(temp);
        context.fileSystem().add(inputFile);        

        when(system2.envVariable("GITLAB_CI")).thenReturn("false");
        when(projectConfiguration.get(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_INSTANCE_URL)).thenReturn(Optional.of("value"));
        when(projectConfiguration.get(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PROJECT_ID)).thenReturn(Optional.of("value"));
        when(projectConfiguration.get(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME)).thenReturn(Optional.of("value"));
        when(projectConfiguration.get(AnalysisDetails.SCANNERROPERTY_PULLREQUEST_BASE)).thenReturn(Optional.of("value"));
        when(projectConfiguration.get(AnalysisDetails.SCANNERROPERTY_PULLREQUEST_BRANCH)).thenReturn(Optional.of("value"));
        when(projectConfiguration.get(AnalysisDetails.SCANNERROPERTY_PULLREQUEST_KEY)).thenReturn(Optional.of("value"));
        when(projectConfiguration.get(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_API_VERSION)).thenReturn(Optional.of("value"));
                
        sensor = new ScannerPullRequestPropertySensor(projectConfiguration, system2);
        sensor.execute(context);

        Map<String, String> properties = context.getContextProperties();

        assertEquals(7, properties.size());        
    }    
}
