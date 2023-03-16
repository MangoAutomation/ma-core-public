Scripts
==========

Scripts modules first of all loads the environment variables by the `getenv.sh` script. Some of the most important variables that are needed are:

- mango_paths_data
- mango_config

Mainly this variables specify the core for mango, where the source code and libraries are; as well as the data base files. All of them are properly known if Mango was set up by an installer, whenever we run a start_mango.sh all required variables must be figured out by this `getenv.sh` if the installetion was done like that.

Another important thing to consider is that we also have a `mango.properties` file where we can change different values; normally this file is populated with lots of variables commented and also it contains a description of each variable value.

Also there are other variables that can be useful to declare in case of deployments of other actions, this variables are:

- mango_paths_data
- keystore
- MA_KEYSTORE
- MA_KEYSTORE_PASSWORD
- MA_KEY_PASSWORD
- MA_KEY_ALIAS

It is important to know that if needed we can customized the value for this variables by setting the value in the `mango.properties`.

Note: We need to make sure the values are properly defined as if not, default values will be considered. This is part of a workaround where we found that default values are taken, so we just need to make sure we are defining the right values for our variables. That it is why additionally some messages are added so at some point the user was able to look at the environment values considered. 

So when running a script it is basic to validate that `mango_paths_home` is correct, it must be the path where we set the Mango installation in the installer. And also we need to check `mango_paths_data` that is where the data base files and sources will be allocated.

The environment values should be printed something like this:



`++ HOME environment variable is /Users/juan.garcia`

`++ mango_paths_home is /Users/juan.garcia/opt/mango`

`+++++ HOME and mango_paths_home are not the same so script may not work properly!! ++++`

`+++++ Mango configuration found on /Users/juan.garcia/mango.properties`

`++ mango_paths_data environment variable is /Users/juan.garcia/opt/mango/data`

`++ mango_script_dir is /Users/juan.garcia/opt/mango/bin`

`++ mango_paths_data is /Users/juan.garcia/opt/mango/data`

`++ keystore variable populated with mango.properties file property ssl.keystore.location`

`++++++ MA_KEYSTORE environment variable populated with default value keystore.p12 as file /Users/juan.garcia/opt/mango/data/keystore.p12 was not found.`

`+++++++ MA_KEYSTORE_PASSWORD environment variable populated with default value freetextpassword`

`++ MA_KEY_PASSWORD environment variable populated with mango.properties file property ssl.key.password`

`+++++ MA_KEY_ALIAS environment variable populated with default value mango`

So the values of course will be loaded from properties file `mango.properties` in the following order:

1- mango_config

2- mango_paths_data/mango.properties

3- mango_paths_data/env.properties

4- HOME/mango.properties

5- mango_paths_home/env.properties

6- mango_paths_home/overrides/properties/env.properties

So if you are facing issues by getting values that are not expected make sure you're following this order. Check your environment variables and mango properties.
 
