## To-do

* Bypass string creation, comparison, and searching by implementing a byte array/bytewise version of each.
* Abstract out file locations. Set permissions on it. Set default file location.
* Further improve interface
	* showing current directory tree
	* also on the android part, add "allow:" and "url", for clarity

* Bandwidth in tests varied from 100-600 kBps, depending on processing or connection/distance from access point.
    Use profiling and refactoring to improve those speeds and make them reliable.