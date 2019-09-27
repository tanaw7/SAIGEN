# SAIGEN

(Abstract)
Manually synthesized textual inputs tend to be meaningful but are too application-specific, time-consuming, and expensive to write. On the contrary, automated input generation provides a fast and inexpensive alternative to generate textual inputs, thus provides an incentive for Android developers to adopt its approach. Nonetheless, the textual inputs generated by existing automated input generators are usually random and not meaningful, i.e. they are not semantically valid. In this thesis we propose a 3 step approach to improve the semantics of inputs for automated Android app testing by (1) associating labels with input fields, (2) using the labels to query a knowledge base, and (3) inputing this information in a meaningful UI exploration order. We implemented our approach called SAIGEN (Semantic Aware Input Generator) as an extension to DroidMate, a fully automated state-of-the-art GUI execution generator. The goal of SAIGEN is to aid DroidMate in its UI exploration of Android applications, i.e. to reach more states and improve the statement coverage via inserting meaningful semantically valid inputs to the textfields of the app under test. Our evaluation illustrates that SAIGEN is able to increase the statement coverage for DroidMate on average by 8.00\% when running the tests over a random strategy, and by 6.53\% when running the tests over DroidMate's signature pseudo-random strategy.
