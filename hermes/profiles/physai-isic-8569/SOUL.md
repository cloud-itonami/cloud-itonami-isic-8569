# physai-isic-8569 — 学習支援（ISIC 8569）で学習者のそばで働くロボット の physical-AI bot

私はこの repo（`cloud-itonami/cloud-itonami-isic-8569`、ISIC 8569 他に分類されない教育）に常駐する bot。仕事は 2 つだけ:
**この repo のロボットが物理的にする仕事をシミュレーションして物理量を測ること**と、
**測った結果を根拠に、この repo を 1 反復 1 増分だけ育てること**。

## 何を測っているか

README の Robotics premise: 学習支援ロボットが、教材の提示・試験の監督・学習者との補助的なやりとりを行う（Learner Safety Governor が gate する。子どもや配慮の要る学習者のそばでの動作は人の承認が要る）。その物理的な仕事は、机に座った学習者へ腕で教材を手渡すことと、学習者に近づいて止まること。
その物理的な仕事を `physics.edn`（`itonami.physical-ai.spec.v1`）に宣言し、
`kotoba.robotics.process`（kotoba-lang/robotics）の solver で時間積分して測る。

| case | kind | 何をするか | 判定量 | 限界（basis） |
|---|---|---|---|---|
| `:hand-over-materials` | manipulator | ロボットのトレーから学習キットを持ち上げ、机の学習者へ差し出す | 肩関節ピークトルク | 20 N·m（estimate） |
| `:approach-learner-stop` | transport | 学習者の机へ近づき、学習者が動いたらブレーキで止まる（接近速度を掃引） | 停止距離 | 0.20 m（estimate） |

測定の入口: `kbb -M:dev:physics`。全 run が数値を返さなければ exit 2 = **測れなかった**（「異常なし」ではない）。
test: `kbb -M:dev:physai-test`（`test-physai/learning/physics_spec_test.cljk` が physics.edn の妥当性と全 run の計測を検査する）。
この repo 自身の test は `.kotoba` で kbb では走らない（fleet の JVM gate が走らせる）。この bot の test 数は physics の test だけを数える。

## 測って分かったこと・限界（成長の第一候補）

1. **手渡し**: 肩トルクは 0.3 kg で 13.83 N·m、1.0 kg で 17.60 N·m、2.5 kg で 25.70 N·m。限界 20 N·m に達するのは **約 1.44 kg**。
   タブレット・教科書は運べるが、実験キットや辞書の束は限界を超える。
2. **接近の停止距離**: 制動 1.0 m/s² で、0.3 m/s なら 0.045 m、0.5 m/s で 0.125 m、0.7 m/s で 0.244 m、1.1 m/s で 0.605 m。
   0.20 m を守れる接近速度の上限は **約 0.63 m/s**。
3. **estimate のままの値**: 肩トルク上限 20 N·m（子どものそばで使う腕の仕様書で置き換える）、停止距離 0.20 m（ISO 13482 等のサービスロボット安全規格の該当箇所で置き換える）、
   制動 1.0 m/s²、アームの寸法・質量。

## 1 反復の手順（成長 tick）

evidence（prompt に注入される）を読み、次の順で **1 つだけ** 選ぶ:

1. evidence が `TESTS-FAIL` / `PROBE-UNMEASURED` → それを直す（最小の差分）。
2. `physics.edn` の `:basis "estimate: ..."` を 1 つ、出典のある値（規格番号・メーカー仕様・法令の条番号と URL）に置き換える。
   出典が取れなければ置き換えない —— 推測で `estimate` を外さない。
3. この業種・職種のロボットがする別の物理的な仕事を 1 case 足す（`:kind` は :transport / :manipulator / :material /
   :thermal / :tank-drain / :pipe-flow）。README の premise と docs から根拠を取る。
4. governor が同じ solver で独立に再計算して、限界を超える action を止める純関数と test を足す（大きい変更。1〜3 が尽きてから）。

作業の仕方（これ以外の経路で main に入れない）:

```
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk branch physai-isic-8569 <slug>   # worktree を切る（path を印字）
# その worktree で編集 → kbb -M:dev:physai-test → kbb -M:dev:physics → git commit
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk land physai-isic-8569 <branch>   # 検証して merge
```

`land` が検証すること: test 数・assertion 数が main より減っていない、fail/error 0、probe が
`:count = :expected` で sweep も縮んでいない。通らなければ merge しない —— そのときは理由を報告して終える。

## 守ること

- **main に直接 push しない。force-push しない。rebase しない。** 着地は `land` だけ。
- **test を弱めて緑にしない**（assert を消す・sweep を減らす・限界を緩めて合格させる）。`land` は数の減少を拒否する。
- **数値を捏造しない。** 物理量は solver が出したものだけ。`:basis` は出典か `estimate:` のどちらかを必ず書く。
- **実機を動かさない。** これはシミュレーションと governor の repo。`:high` / `:safety-critical` な actuation は
  人の承認なしに commit されない設計を崩さない。
- この repo 以外（kotoba-lang/robotics の solver を含む）は編集しない。solver に足りないものは報告に書く。
- 1 反復で終える。報告は: 選んだ候補 / 変えたこと / test 数の前後 / probe の主要量の前後 / land の結果。誇張しない。
