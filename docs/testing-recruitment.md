# Recruiting the 12 closed-test testers

Google requires **personal** Play developer accounts created after 13 November 2023 to run a closed test with **12 testers opted in continuously for 14 days** before production access can be requested. Organization accounts are exempt.

## What a tester actually has to do

1. Open the opt-in link you send them (from the closed track's Testers tab, "Join on the web").
2. Sign in with a Google account and tap **Accept invite**.
3. Install NeonPlayer from the Play Store link.
4. Leave it installed and stay opted in for 14 days. Using it is encouraged but not enforced.

They do not need to be technical, own a specific phone, or live in your country. They need an Android phone and a Google account. One account per person; duplicate accounts you create yourself are detected and risk account termination.

## Where to find them

- Family, friends, coworkers, classmates: the easiest 5-8.
- A synthwave, retrowave or Android community you are already in.
- Local maker or coding meetups.

### Reddit

Tester-swap subreddits, where developers join each other's tests in exchange for joining yours:

| Subreddit | Notes |
|---|---|
| r/AndroidClosedTesting | Most focused on the Play 12-tester rule; daily activity, tester swaps |
| r/AndroidAppTesters | Long-standing, explicitly "test for test"; uses flairs like `[12 Testers Needed]` |
| r/TestersCommunity | Smaller, dedicated to the 14-day requirement |
| r/alphaandbetausers | General beta recruitment; busy with other developers |
| r/betatests | 20k+ members, structured self-promotion rules |
| r/TestMyApp | App testing only, no paid services |
| r/BetaTestersNeeded | Small and focused, higher conversion |

Rules that matter on all of them:

- **Reciprocate.** These are give-and-take. Posting a link and never testing anyone else's app gets you ignored.
- **Read the sidebar and pinned post first.** Most require a specific title format, a minimum account age or karma, and no cross-posting the same text to several subs at once.
- **New accounts get shadowbanned.** If your Reddit account is new or low-karma, comment in a few threads before posting your own.
- **Recruit 15-20, not 12.** People drop out, and a dropout resets that tester's clock.
- **Use a Google Group** for the tester list so people can join themselves instead of you collecting addresses by hand.

Niche communities usually beat tester subs for real feedback: a synthwave or retrowave sub will have people who actually want the app, while tester subs are mostly other developers.

## Google Groups instead of collecting emails

Play Console lets a closed test pull its tester list from a Google Group instead of a hand-typed email list. From Google's docs: *"With a closed test, you can manage testers with email lists or Google Groups... You can also create a personal group or use a Google Workspace group connected to your developer account."* Google Groups have **no size limit**; email lists cap at 2,000 testers per list, 200 lists per developer account and 50 per track.

**What it changes:** you never collect or type individual addresses. You publish one group join link, people add themselves, and Play reads membership from the group.

**What it does not change:** each tester still needs a Google Account or Google Workspace account, and Google states that *"for closed tests using a Google Group, users must join the group before opting into your test."* So the tester does two steps instead of one: join the group, then open the opt-in link.

Setup outline:

1. groups.google.com → **Create group**, name it something like `neonplayer-testers`.
2. Set **Who can join group** to allow people to join themselves (for example "Anyone can join"), otherwise you are back to inviting each address by hand.
3. Play Console → Closed testing → Alpha → **Testers** tab → **Google Groups** → add the group address.
4. Share the group join link and the opt-in link together: https://play.google.com/apps/testing/com.neonplayer

Use a plain personal Gmail account to own the group. Some developers report Play Console failing to resolve groups owned by Google Workspace domains.

## Post you can paste

> **Looking for 12 testers for a synthwave music player (Android)**
>
> I built NeonPlayer, a landscape-first music player for your own local files. Prop your phone sideways and Stage Mode turns it into a now-playing display: big album art, title, and a live visualizer, with the controls fading out of the way. It also has an equalizer, playlists, a drag-reorder queue and looping video backdrops. No ads, no accounts, no internet permission, nothing collected.
>
> Google requires 12 testers opted in for 14 days before I can publish it publicly. If you have an Android phone, all you would do is tap a link, install it from Play, and keep it installed for two weeks. Feedback is welcome but optional.
>
> Link: https://play.google.com/apps/testing/com.neonplayer

## Links

- Closed testing opt-in (counts toward the 12): https://play.google.com/apps/testing/com.neonplayer
- Internal testing opt-in (does not count): https://play.google.com/apps/internaltest/4700958626991204136

Both are derived from the package name or a fixed track ID, so they stay the same across releases.

## Timeline

- Day 0: publish the closed testing release, add the 12 emails, send the link.
- Day 0-2: testers opt in and install. The 14-day clock starts per tester when they opt in, so get everyone in early.
- Day 14+: the Dashboard checklist turns green and **Apply for production** becomes clickable.
- Then answer the three question groups and wait for review, usually seven days or less.

## If 12 testers is not realistic

- **Stay on GitHub releases.** The signed APK is already public and needs no gate. You lose Play's update mechanism and discovery, nothing else.
- **Internal testing.** Up to 100 testers, no 14-day rule, no production application. Good for installing your own builds from Play, never public.
- **Organization account.** Exempt from the 12-tester rule, but requires a D-U-N-S number (free, can take up to two weeks) and a business identity. Ask Play support whether your existing account can be converted rather than assuming it is self-serve.
