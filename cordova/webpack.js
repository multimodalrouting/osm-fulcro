const webpack = require('webpack');
const CopyWebpackPlugin = require('copy-webpack-plugin');


const path = require('path');

function resolvePath(dir) {
  return path.join(__dirname, '..', dir);
}

const env = process.env.NODE_ENV || 'development';
const target = process.env.TARGET || 'web';

const webpackConf = {
  mode: env,
  entry: [
    './cordova/www/js/main/main.js',
  ],
  output: {
    path: resolvePath('cordova/www')
  },
  devtool: env === 'production' ? 'source-map' : 'eval',
  devServer: {
    hot: true,
    open: true,
    compress: true,
    contentBase: '/cordova/www/',
    disableHostCheck: true,
    watchOptions: {
      poll: 1000,
    },
  },
  module: {
    rules: [],
  },
  plugins: [
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(env),
      'process.env.TARGET': JSON.stringify(target)
    }),
  new CopyWebpackPlugin([
    {
      from: resolvePath('resources/public/css'),
      to: resolvePath('cordova/www/css')
    },
    {
      from: resolvePath('node_modules/d3/dist/d3.min.js'),
      to: resolvePath('cordova/www/d3.min.js')
    },
    {
      from: resolvePath('node_modules/semantic-ui/dist/semantic.min.css'),
      to: resolvePath('cordova/www/css/semantic.min.css')
    },
    {
      from: resolvePath('node_modules/@fortawesome/fontawesome-free/css/all.css'),
      to: resolvePath('cordova/www/css/fontawesome-all.css')
    },
    {
      from: resolvePath('node_modules/@fortawesome/fontawesome-free/webfonts'),
      to: resolvePath('cordova/www/webfonts')
    },

  ])


]
};

webpack(webpackConf, (err, stats) => {
    if (err) throw err;

    process.stdout.write(`${stats.toString({
      colors: true,
      modules: false,
      children: false, // If you are using ts-loader, setting this to true will make TypeScript errors show up during build.
      chunks: false,
      chunkModules: false,
    })}\n\n`);

    if (stats.hasErrors()) {
      console.log('Build failed with errors.\n');
      process.exit(1);
    }

    console.log('Build complete.\n');
});


