/**
 * Copyright@2011 wro4j
 */
package ro.isdc.wro.runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.config.Context;
import ro.isdc.wro.config.jmx.ConfigConstants;
import ro.isdc.wro.config.jmx.WroConfiguration;
import ro.isdc.wro.extensions.processor.css.CssLintProcessor;
import ro.isdc.wro.extensions.processor.css.YUICssCompressorProcessor;
import ro.isdc.wro.extensions.processor.js.JsHintProcessor;
import ro.isdc.wro.extensions.processor.support.csslint.CssLintException;
import ro.isdc.wro.extensions.processor.support.linter.LinterException;
import ro.isdc.wro.model.resource.processor.factory.ConfigurableProcessorsFactory;
import ro.isdc.wro.model.resource.processor.impl.css.CssMinProcessor;
import ro.isdc.wro.model.resource.processor.impl.css.CssUrlRewritingProcessor;
import ro.isdc.wro.model.resource.processor.impl.css.CssVariablesProcessor;
import ro.isdc.wro.model.resource.processor.impl.js.JSMinProcessor;
import ro.isdc.wro.model.resource.support.AbstractConfigurableMultipleStrategy;
import ro.isdc.wro.model.resource.support.naming.ConfigurableNamingStrategy;
import ro.isdc.wro.model.resource.support.naming.TimestampNamingStrategy;
import ro.isdc.wro.util.WroUtil;


/**
 * @author Alex Objelean
 */
public class TestWro4jCommandLineRunner {
  private static final Logger LOG = LoggerFactory.getLogger(TestWro4jCommandLineRunner.class);
  private File destinationFolder;

  @Before
  public void setUp() {
    destinationFolder = new File(FileUtils.getTempDirectory(), "wroTemp-" + new Date().getTime());
    destinationFolder.mkdir();
  }

  @After
  public void tearDown() {
    FileUtils.deleteQuietly(destinationFolder);
  }

  @Test
  public void cannotProcessWrongArgument()
      throws Exception {
    try {
      final String[] args = new String[] {
        "-wrongArgument"
      };
      invokeRunner(args);
      fail("Should have failed!");
    } catch (final Exception e) {
      assertEquals(CmdLineException.class, e.getCause().getClass());
    }
  }

  @Test
  public void cannotProcessNoArguments()
      throws Exception {
    try {
      invokeRunner("".split(" "));
    } catch (final Exception e) {
      assertEquals(CmdLineException.class, e.getCause().getClass());
    }
  }

  @Test
  public void processCorrectArguments()
      throws Exception {
    invokeRunner(createValidArguments());
  }

  protected String[] createValidArguments() {
    final String wroFile = getValidWroFile();
    final String[] args = String.format("--wroFile %s --contextFolder %s -m ", new Object[] {
      wroFile, getValidContextFolder()
    }).split(" ");
    return args;
  }

  private void invokeRunner(final String[] args)
      throws Exception {
    new Wro4jCommandLineRunner() {
      {
        {
          setDestinationFolder(destinationFolder);
        }
      }

      @Override
      protected void onRunnerException(final Exception e) {
        throw WroRuntimeException.wrap(e);
      }
    }.doMain(args);
  }

  @Test
  public void shouldApplyCssUrlRewriterProperly()
      throws Exception {
    final String contextFolder = new File(getClass().getResource("").getFile()).getAbsolutePath();

    final String processorsList = AbstractConfigurableMultipleStrategy.createItemsAsString(CssUrlRewritingProcessor.ALIAS);
    final String[] args = String.format("--wroFile %s --contextFolder %s -m --preProcessors " + processorsList,
        new Object[] {
          getValidWroFile(), contextFolder
        }).split(" ");
    invokeRunner(args);
  }

  @Test
  public void shouldUseMultiplePreProcessors()
      throws Exception {
    invokeMultipleProcessors("--preProcessors");
  }

  @Test
  public void shouldUseMultiplePostProcessors()
      throws Exception {
    invokeMultipleProcessors("--postProcessors");
  }

  private void invokeMultipleProcessors(final String processorsType)
      throws Exception {
    final String processorsList = AbstractConfigurableMultipleStrategy.createItemsAsString(CssMinProcessor.ALIAS,
        JSMinProcessor.ALIAS, CssVariablesProcessor.ALIAS);
    final String[] args = String.format(
        "--wroFile %s --contextFolder %s --destinationFolder %s -m %s " + processorsList, new Object[] {
          getValidWroFile(), getValidContextFolder(), destinationFolder.getAbsolutePath(), processorsType
        }).split(" ");
    invokeRunner(args);
  }

  @Test(expected = CssLintException.class)
  public void shouldApplyCssLint()
      throws Exception {
    final String[] args = String.format(
        "--wroFile %s --contextFolder %s --destinationFolder %s -m -c " + CssLintProcessor.ALIAS, new Object[] {
          getValidWroFile(), getValidContextFolder(), destinationFolder.getAbsolutePath()
        }).split(" ");
    invokeRunner(args);
  }

  @Test
  public void shouldApplyYuiCssMinAsPostProcessor()
      throws Exception {
    final String[] args = String.format(
        "--wroFile %s --contextFolder %s --destinationFolder %s -m --postProcessors " + YUICssCompressorProcessor.ALIAS,
        new Object[] {
          getValidWroFile(), getValidContextFolder(), destinationFolder.getAbsolutePath()
        }).split(" ");
    invokeRunner(args);
  }

  @Test(expected = LinterException.class)
  public void shouldApplyJsHint()
      throws Exception {
    final String[] args = String.format(
        "--wroFile %s --contextFolder %s --destinationFolder %s -m -c " + JsHintProcessor.ALIAS, new Object[] {
          getValidWroFile(), getValidContextFolder(), destinationFolder.getAbsolutePath()
        }).split(" ");
    invokeRunner(args);
  }

  @Test
  public void shouldProcessTestWroXml()
      throws Exception {
    final String[] args = String.format("-m --wroFile %s --contextFolder %s --destinationFolder %s", new Object[] {
      getValidWroFile(), getValidContextFolder(), destinationFolder.getAbsolutePath()
    }).split(" ");
    invokeRunner(args);
  }

  private String getValidWroFile() {
    return getValidContextFolder() + File.separator + "wro.xml";
  }

  private String getValidContextFolder() {
    return new File(getClass().getResource("").getFile()).getAbsolutePath();
  }

  @Test
  public void shouldAcceptGroovyDSLUsingSmartModelFactory() {
    final File contextFolderFile = new File(getClass().getResource("").getFile(), "dsl");
    final String contextFolder = contextFolderFile.getAbsolutePath();

    final String[] args = String.format("-m --contextFolder %s --destinationFolder %s", new Object[] {
      contextFolder, destinationFolder.getAbsolutePath()
    }).split(" ");

    // invoke runner
    new Wro4jCommandLineRunner() {
      {
        {
          setDestinationFolder(destinationFolder);
        }
      }

      @Override
      protected File newDefaultWroFile() {
        return new File(contextFolderFile, "wro.xml");
      }

      @Override
      protected void onRunnerException(final Exception e) {
        LOG.error("Exception occured: ", e.getCause());
        throw new RuntimeException(e);
      }
    }.doMain(args);
  }

  @Test
  public void shouldUseDefaultConfigurationWhenConfigFileDoesNotExist() {
    final WroConfiguration expected = new WroConfiguration();
    new Wro4jCommandLineRunner() {
      @Override
      protected File newWroConfigurationFile() {
        return new File("/path/to/invalid/file");
      }

      @Override
      void doProcess()
          throws IOException {
        // do nothing
        assertEquals(expected, Context.get().getConfig());
      };
    }.doMain(createValidArguments());
  }

  @Test
  public void shouldLoadWroConfigurationFromCustomLocation()
      throws Exception {
    final File wroConfigurationFile = WroUtil.createTempFile();
    try {
      final String[] args = String.format(
          "-m --wroConfigurationFile %s --wroFile %s --contextFolder %s --destinationFolder %s", new Object[] {
            wroConfigurationFile.getPath(), getValidWroFile(), getValidContextFolder(), destinationFolder.getAbsolutePath()
          }).split(" ");
      invokeRunner(args);
      try {
        final String preProcessorsConfig = String.format("%s=%s", ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS,
            "invalid");
        FileUtils.write(wroConfigurationFile, preProcessorsConfig);
        invokeRunner(args);
        Assert.fail("should have failed");
      } catch (final WroRuntimeException e){
      }
    } finally {
      FileUtils.deleteQuietly(wroConfigurationFile);
    }
  }

  @Test(expected = WroRuntimeException.class)
  public void shouldFailWhenInvalidProcessorConfiguredInWroProperties()
      throws Exception {
    final File wroFile = new File(getValidWroFile());
    final File contextFolder = new File(getValidContextFolder());
    final File wroConfigurationFile = WroUtil.createTempFile();

    try {
      final String preProcessorsConfig = String.format("%s=%s", ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS,
          "invalid");
      FileUtils.write(wroConfigurationFile, preProcessorsConfig);
      executeRunner(wroFile, contextFolder, wroConfigurationFile);
    } finally {
      FileUtils.deleteQuietly(wroConfigurationFile);
    }
  }

  @Test(expected = WroRuntimeException.class)
  public void shouldFailWhenInvalidNamingStrategyConfiguredInWroProperties()
      throws Exception {
    final File wroFile = new File(getValidWroFile());
    final File contextFolder = new File(getValidContextFolder());
    final File wroConfigurationFile = WroUtil.createTempFile();

    try {
      final String namingStrategyConfig = String.format("%s=%s", ConfigurableNamingStrategy.KEY, "invalid");
      FileUtils.write(wroConfigurationFile, namingStrategyConfig);
      executeRunner(wroFile, contextFolder, wroConfigurationFile);
    } finally {
      FileUtils.deleteQuietly(wroConfigurationFile);
    }
  }

  @Test
  public void shouldApplyNamingStrategyConfiguredInWroProperties()
      throws Exception {
    final File wroFile = new File(getValidWroFile());
    final File contextFolder = new File(getValidContextFolder());
    final File wroConfigurationFile = WroUtil.createTempFile();

    try {
      final String namingStrategyConfig = String.format("%s=%s", ConfigurableNamingStrategy.KEY,
          TimestampNamingStrategy.ALIAS);
      FileUtils.write(wroConfigurationFile, namingStrategyConfig);
      executeRunner(wroFile, contextFolder, wroConfigurationFile);
    } finally {
      FileUtils.deleteQuietly(wroConfigurationFile);
    }
  }

  @Test
  public void shouldApplyProcessorConfiguredInWroProperties()
      throws Exception {
    final File wroFile = new File(getValidWroFile());
    final File contextFolder = new File(getValidContextFolder());
    final File wroConfigurationFile = WroUtil.createTempFile();

    try {
      final String preProcessorsConfig = String.format("%s=%s", ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS,
          JSMinProcessor.ALIAS);
      FileUtils.write(wroConfigurationFile, preProcessorsConfig);
      executeRunner(wroFile, contextFolder, wroConfigurationFile);
    } finally {
      FileUtils.deleteQuietly(wroConfigurationFile);
    }
  }

  private void executeRunner(final File wroFile, final File contextFolder, final File wroConfigurationFile) {
    new Wro4jCommandLineRunner() {
      {
        {
          setDestinationFolder(destinationFolder);
        }
      }

      @Override
      protected File getContextFolder() {
        return contextFolder;
      };

      @Override
      protected File newWroConfigurationFile() {
        return wroConfigurationFile;
      }

      @Override
      protected File newDefaultWroFile() {
        return wroFile;
      };

      @Override
      protected void onRunnerException(final Exception e) {
        throw WroRuntimeException.wrap(e);
      }
    }.doMain(new String[] {});
  }

  @Test
  public void shouldUseProperConfigurationsWhenConfigFileExist() {
    final File temp = WroUtil.createTempFile();
    final WroConfiguration expected = new WroConfiguration();
    expected.setConnectionTimeout(10000);
    expected.setDisableCache(true);
    expected.setEncoding("ISO-8859-1");
    try {
      new Wro4jCommandLineRunner() {
        @Override
        protected File newWroConfigurationFile() {
          try {
            final Properties props = new Properties();
            props.setProperty(ConfigConstants.connectionTimeout.name(), "10000");
            props.setProperty(ConfigConstants.disableCache.name(), "true");
            props.setProperty(ConfigConstants.encoding.name(), "ISO-8859-1");
            props.list(new PrintStream(new FileOutputStream(temp)));
            return temp;
          } catch (final IOException e) {
            throw WroRuntimeException.wrap(e);
          }
        }

        @Override
        void doProcess()
            throws IOException {
          // do nothing
          assertEquals(expected, Context.get().getConfig());
        };
      }.doMain(createValidArguments());
    } finally {
      FileUtils.deleteQuietly(temp);
    }
  }
}
