import {TreeNode} from '@atomist/rug/tree/PathExpression'

export interface Travis extends TreeNode {

  encrypt(repo: String, token: String, org: String, content: String): void

  enable(repo: String, token: String, org: String): void

  disable(repo: String, token: String, org: String): void
}
