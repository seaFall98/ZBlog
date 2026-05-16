1.后台http://localhost:4000/login是纯白界面，没有具体界面

这个问题之前就提过，是复现了吗？

![image-20260515221704960](C:\Users\seaFall98\AppData\Roaming\Typora\typora-user-images\image-20260515221704960.png)



2.上方的Smoke是什么？我在FlecBlog里没见到这个功能

图1是我们的

![image-20260515221810989](C:\Users\seaFall98\AppData\Roaming\Typora\typora-user-images\image-20260515221810989.png)

图2是flecblog

![image-20260515221850981](C:\Users\seaFall98\AppData\Roaming\Typora\typora-user-images\image-20260515221850981.png)

图3 点进smoke报错404 http://localhost:3000/smoke

![image-20260515222645421](C:\Users\seaFall98\AppData\Roaming\Typora\typora-user-images\image-20260515222645421.png)

注意：我觉得flecblog的网站源码可能跟我们从github拉取的flecblog的仓库源码并不同步，包括我们可能做了一些重构和修改，所以我并不是纠结这些地方不同，不是要求一定要一致，而是确保工作内容没有混乱，没有乱做功能或者少做功能

3.底栏也跟flecblog有很多不同

图1是我们的

![image-20260515222214249](C:\Users\seaFall98\AppData\Roaming\Typora\typora-user-images\image-20260515222214249.png)

图2是flecblog的

![image-20260515222157594](C:\Users\seaFall98\AppData\Roaming\Typora\typora-user-images\image-20260515222157594.png)

我觉得我们的明显有重复、有遗漏，flecblog的更科学。这里是源码不同步的问题还是代码重构的问题？我更倾向于做成flecblog那样。

尤其是http://localhost:3000/feedback 反馈投诉，flecblog底栏有明确的入口，zblog我没找到入口

我手动输入url，是有反馈投诉这个页面的

![image-20260515222833542](C:\Users\seaFall98\AppData\Roaming\Typora\typora-user-images\image-20260515222833542.png)

