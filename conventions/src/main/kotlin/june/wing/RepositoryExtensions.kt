package june.wing

import org.gradle.api.artifacts.dsl.RepositoryHandler

/**
 * Repository 相关扩展函数
 * 用于配置 Gradle 仓库
 */

/**
 * 将仓库添加到列表的第一位
 */
fun RepositoryHandler.addRepositoryFirst(addRepoAction: RepositoryHandler.() -> Unit) {
    addRepoAction()
    if (size > 1) {
        val removeLast = removeLast()
        addFirst(removeLast)
    }
}

/**
 * 配置中国镜像源仓库
 * 包含腾讯镜像和 5hmlA 私有仓库
 */
fun RepositoryHandler.chinaRepos() {
    if (isEmpty()) {
        //google和maven应该都是默认添加的
        //name:Google
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        //name:MavenRepo
        mavenCentral()
    }
    if (findByName("5hmlA") != null) {
        //已经设置过不再需要设置
        return
    }
    addRepositoryFirst {
        maven {
            name = "tencent"
            isAllowInsecureProtocol = true
            setUrl("https://mirrors.tencent.com/nexus/repository/maven-public/")
            content {
                //https://blog.csdn.net/jklwan/article/details/99351808
                excludeGroupByRegex("osp.spark.*")
                excludeGroupByRegex("osp.june.*")
                excludeGroupByRegex("osp.gene.*")
                excludeGroup("aar")
            }
        }
    }

    //限定指定规则的group只访问5hmlA仓库
    maven {
        name = "5hmlA"
        isAllowInsecureProtocol = true
        setUrl("https://maven.pkg.github.com/5hmlA/sparkj")
        credentials {
            // https://www.sojson.com/ascii.html
            username = "5hmlA"
            password =
                "\u0067\u0068\u0070\u005f\u004f\u0043\u0042\u0045\u007a\u006a\u0052\u0069\u006e\u0043\u0065\u0048\u004c\u0068\u006b\u0052\u0036\u0056\u0061\u0041\u0074\u0068\u004f\u004a\u0059\u0042\u0047\u0044\u0073\u0049\u0032\u0070\u0064\u0064\u0069\u0066"
        }
        //只有以下规则的group才会访问5hmlA仓库
        content {
            //https://blog.csdn.net/jklwan/article/details/99351808
            includeGroupByRegex("osp.spark.*")
            includeGroupByRegex("osp.june.*")
            includeGroupByRegex("osp.gene.*")
        }
    }
    //content有这些
    // excludeGroup：在这个库中不搜索这个group，如my.company，但是只会匹配my.company，如果是my.company.module则不匹配
    // excludeGroupByRegex：类似excludeGroup，但是可以使用正则表达式，如my.company.*可以匹配my.company和my.company.module。
    // includeGroup：在这个库中搜索包含这个group，类似excludeGroup精确匹配
    // includeGroupByRegex：使用方法同excludeGroupByRegex
}

