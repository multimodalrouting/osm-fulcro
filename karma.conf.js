const isDocker = require('is-docker')();

module.exports = function (config) {
  config.set({
    customLaunchers: {
      ChromeCustom: {
        base: 'ChromeHeadless',
        // We must disable the Chrome sandbox when running Chrome inside Docker (Chrome's sandbox needs
        // more permissions than Docker allows by default)
        flags: isDocker ? ['--no-sandbox'] : []
      }
    },
    // The directory where the output file lives
    basePath: 'target',
    // The file itself
    files: ['ci.js'],
    frameworks: ['cljs-test'],
    plugins: ['karma-cljs-test', 'karma-chrome-launcher'],
    colors: true,
    logLevel: config.LOG_INFO,
    client: {
      args: ["shadow.test.karma.init"],
      singleRun: true
    }
  })
};
