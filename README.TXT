* What is this?

A basic set of command-line tools for managing a Wordpress blog.

* Why?

A number of reasons.

I often want to compose a blog post while commuting (cool your jets, distracted
driving campaigners, I commute by ferry), but my mobile Internet is just
unreliable enough (and Wordpress's flaky Javascript is just fault-intolerant
enough) that attempting to do so via a browser often results in a
mostly-composed post being lost… forever.

There's tools out there to allow for off-line editing of blog posts, of course,
but they all cost money and I'm a notorious cheapskate. Plus most of them don't
run on a Mac, and I use Macs. Plus most of them do *way* more than I need. I
don't need a whole new editor to learn. I just need to send text I can type in
an existing text editor I already know to my blog.

I don't program in Java for my day job yet I want to keep my Java skills
reasonably sharp. This seemed like just the right size of project to do that.
And it would produce something practical I'd use regularly.

* How much does this cost?

Nothing! I've decided to make it open-source.

* Do I have to compile it?

No. There's a reasonably recent .jar file here containing everything you need
to run this code.

* Does it run on Linux? On a Mac? On Windows?

It runs on all three! It's written in Java, so it runs anywhere that Java will
run.

* What about Unicode support?

It's baked-in. It reads and writes characters based on whatever your locale's
default character set is in, and talks to the server in Unicode.

* So it's command-line based? That sucks! I want a GUI!

Then write one; the source code is right here as a starting point for you. Or
buy one of the already-existing offline blog-editing tools that have a GUI.

* What's the URL I should use?

Typically the URL of your blog with the string "/xmlrpc.php" appended, though
that can vary depending on how Wordpress is configured.

* What can it do?

ListBlogs - list all blogs on a site
ListPosts - list the most recent posts (by default 10, but selectable via an
            option) on a blog, in reverse chronological order
ListAuthors - list post authors
GetPost - retrieve one or more posts
NewPost - create a new post
EditPost - modify an existing post
DeletePost - delete one or more existing posts

* How do I run a command?

It varies depending on how you choose to install things but typically it's
something like:
      java -cp wptools.jar wptools.cmds.ListBlogs

* How do I find out more about the options of each command?

If you supply the "--help" option, each command will print a summary of
available options and exit.

* How do "terms" work? How do I specify a category?

"Terms" as Wordpress defines them are names pertaining to a given taxonomy.
Categories are, surprisingly enough, in the taxonomy "category". The "terms"
field that GetPost displays (and NewPost reads) is a comma-delimited list of
taxonomy.name pairs. So to post something in the category "Computers," you
would specify "--terms=category.Computers" to NewPost. Note that both taxonomies
and the names within them are case-sensitive.

* I used NewPost and my post didn't show up!

If you don't specify a post status, you get the default status for a new post,
which is typically "draft". You need to specify --status=publish for a post to
be published. An existing draft post can be published with a command like:
      java -cp wptools.jar wptools.cmds.EditPost --status=publish --no-content id

* I don't want to specify my blog's XMLRPC URL, username, etc. each time I run
a command.

If you create a wptools.properties file in your home directory, you don't have
to. You can specify blogid, url, and username there.

* I have multiple blogs.

Then use blog groups in wptools.properties. If you create a set of properties
called blog1.url, blog1.blogid, and blog1.username, then you can specify
--group=blog1 and the parameters from that group will be used.

* I put "password=MyBigS3cret!" in wptools.properties and it was ignored!

Storage of passwords is not allowed because it is not secure. If you must stop
these tools from prompting for a password (for example, because you have a shell
script that invokes multiple ones and you don't want to be asked multiple
times), you can set the WPTOOLS_PASS environment variable to the password. Note
that you should avoid just embedding a plaintext password in a script as that is
just as insecure as one in a properties file. Instead, prompt for it, then set
the environment variable.

* How do I do certificate management for HTTPS?

Currently you don't. Certs are always blindly trusted. This is a deficiency
that should be fixed.
