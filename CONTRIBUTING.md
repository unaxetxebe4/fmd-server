# Contributing to FindMyDevice

Thanks for taking the time to improve FindMyDevice!

Contributions in all fields of development are welcome.

If you are unsure where to start here are some ideas:
- look at the issues of the Project
- start translating the application into your language
- imlement a new feature you had in mind.

If you have questions or your merge request isn't ready you can upload it and mark it as a WOP(WorkInProgress).

## How to start and upload your changes?

1. Fork the Project
2. Make your changes to your Fork
3. Create a MergeRequest on GitLab and write a short description what you have improved
4. I will check it and if everything seems fine you have contributed. :description

## Translating

Translations are hosted on [Weblate](https://hosted.weblate.org/projects/findmydevice/fmd-android/).

The source language is English, located in `values/strings.xml`.
English translations should always be manually committed to Gitlab to keep the file clean and organised.
English is synced up from Gitlab to Weblate, using a webhook on every commit to master.

All other languages should be added and edited on Weblate.
They are then synced down from Weblate to Gitlab, Weblate will automatically open Merge Requests.

